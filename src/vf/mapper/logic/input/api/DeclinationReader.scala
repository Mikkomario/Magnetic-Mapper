package vf.mapper.logic.input.api

import utopia.annex.model.response.{RequestFailure, Response}
import utopia.bunnymunch.jawn.JsonBunny
import utopia.disciple.controller.RequestRateLimiter
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.TryFuture
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.Model
import utopia.flow.operator.EqualsExtensions._
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.async.Volatile
import utopia.paradigm.angular.Rotation
import vf.mapper.model.coordinate.{CircleGrid, MagneticMapPoint, MapPoint}

import java.nio.file.Path
import scala.concurrent.ExecutionContext
import scala.util.Success

object DeclinationReader
{
	/**
	 * Initializes a new declination reader, attempting to read stored data from a local file first
	 * @param api Api used for reading declination data
	 * @param grid Grid used for grouping map points together
	 * @param dataStoreDirectory Directory where declination data files are stored
	 * @param exc Implicit execution context
	 * @param logger Implicit logger for recording non-critical errors
	 * @return A new declination reader
	 */
	def initialize(api: DeclinationApi, grid: CircleGrid, dataStoreDirectory: Path)
	              (implicit exc: ExecutionContext, logger: Logger) =
	{
		// Finds a json file that contains declination data for a matching grid
		val pathAndModel = dataStoreDirectory.iterateChildren { _.filter { _.fileType ~== "json" }.findMap { path =>
			JsonBunny.munchPath(path).toOption.flatMap { _.model }
				.filter { _("grid").model.exists { gridModel => CircleGrid(gridModel).toOption.contains(grid) } }
				.map { path -> _ }
		} }.getOrMap { error =>
			logger(error, "Failed to read the data store directory")
			None
		}
		pathAndModel match {
			// Case: Existing data found => reads it
			case Some((path, model)) => parse(grid, model, api, path)
			// Case: No existing data found => Starts without pre-loaded data
			case None =>
				new DeclinationReader(api, grid,
					dataStorePath = Some(dataStoreDirectory/s"declinations-${grid.circlesUntilRadius}-${
						grid.innerCircleSectors}.json"))
		}
	}
	
	/**
	 * Initializes a new declination reader by reading previously saved settings
	 * @param file               File from which declination data will be read
	 * @param api                Api used for reading declination data
	 * @param exc                Implicit execution context
	 * @param logger             Implicit logger for recording non-critical errors
	 * @return A new declination reader
	 */
	def loadFrom(file: Path, api: => DeclinationApi)(implicit exc: ExecutionContext, logger: Logger) =
		JsonBunny.munchPath(file).flatMap { v =>
			val model = v.getModel
			CircleGrid(model("grid").getModel).map { grid => parse(grid, model, api, file) }
		}
	
	private def parse(grid: CircleGrid, model: Model, api: DeclinationApi, path: Path)
	                 (implicit exc: ExecutionContext, logger: Logger) =
	{
		// Data parsing may fail (logs errors)
		val data = model("data").getVector.map { v =>
			val model = v.getModel
			model("index").tryPairWith { _.tryInt }
				.flatMap { index => MagneticMapPoint(model("point").getModel).map { index -> _ } }
		}.toTryCatch.logToTry.logToOption match {
			case Some(data) => data.toMap
			case None => Map[Pair[Int], MagneticMapPoint]()
		}
		new DeclinationReader(api, grid, data, Some(path))
	}
}

/**
 * Used for reading magnetic declination information from a remote API
 * @author Mikko Hilpinen
 * @since 24.1.2023, v0.1
 * @param api Api used for reading declination data
 * @param grid Grid used for grouping map points together
 * @param preloadedDeclinations Pre-loaded declination data [grid index -> cell center]
 * @param dataStorePath Path to the json file where read data should be stored
 */
class DeclinationReader(api: DeclinationApi, grid: CircleGrid,
                        preloadedDeclinations: Map[Pair[Int], MagneticMapPoint] = Map(),
                        dataStorePath: Option[Path] = None)
                       (implicit exc: ExecutionContext)
{
	// ATTRIBUTES   ------------------------
	
	// Parses declination values from json responses
	private implicit val declinationParser: FromModelFactory[Rotation] = FromModelFactory { model =>
		// Result is expected to be an object array with one element
		model("result").getVector.headOption
			.toTry { new NoSuchElementException(s"No result listed in response: $model") }
			.flatMap { result =>
				// The declination property is required
				result("declination").double
					.toTry { new NoSuchElementException(s"No declination listed in result: $result") }
					.map { declination =>
						// Parses the unit, if found
						// Default unit is degrees
						if (model("units")("declination").getString.toLowerCase.startsWith("rad"))
							Rotation.ofRadians(declination)
						else
							Rotation.ofDegrees(declination)
					}
			}
	}
	
	// The API accepts a total of 50 requests per second, but this is divided between ALL clients
	private val requestLimiter = RequestRateLimiter.maxPerSecond(15)
	// Stores read declinations in order to minimize requests
	private val declinationsPointer = Volatile(preloadedDeclinations)
	
	
	// OTHER    ----------------------------
	
	/**
	 * Magnetizes a map point by attaching declincation information.
	 * NB: 60+ degrees south will not be calculated, but a 0 degree declination will be used instead.
	 * @param point A point to "magnetize"
	 * @return A future that returns a "magnetized" copy of the specified point (i.e. declination included).
	 *         The future will contain a failure if declination-requesting failed.
	 */
	def magnetize(point: MapPoint) = {
		// For now, anything beyond the 60th south parallel is ignored
		if (point.latitude.clockwiseDegrees >= 60)
			TryFuture.success(point.magnetic(Rotation.zero))
		else {
			// Checks whether a close value has already been retrieved
			val index = grid.cellIndexOf(point)
			declinationsPointer.value.get(index) match {
				// Case: Declination already available => Returns
				case Some(preloaded) => TryFuture.success(point.magnetic(preloaded.declination))
				// Case: No declination available => Requests a new declination value
				case None =>
					// Requests declination for the grid cell center-point
					val cellCenter = grid.centerOf(index)
					val resultFuture = requestLimiter.push { api.getDeclinationAt(cellCenter.latLongDegrees) }.map {
						// Case: Successful (2XX) response => Parses declination from the response
						case Response.Success(_, body, _) =>
							body.parsedSingle.map { declination =>
								// Stores the read declination value
								declinationsPointer.update { _ + (index -> cellCenter.magnetic(declination)) }
								point.magnetic(declination)
							}
						// Case: Failed request or response => Fails
						case f: RequestFailure => f.toFailure
					}
					// If any request fails, terminates all pending requests as well
					resultFuture.foreachFailure { _ => requestLimiter.stop() }
					resultFuture
			}
		}
	}
	
	/**
	 * Saves the read declination data to a local file, but only if the file has already been specified.
	 * @return Success or failure. Success contains the written file's path, if available.
	 */
	def saveStatus() = {
		dataStorePath match {
			case Some(path) =>
				path.createParentDirectories().flatMap { _.writeJson(Model.from(
					"grid" -> grid,
					"data" -> declinationsPointer.value.iterator.map { case (index, point) =>
						Model.from("index" -> index, "point" -> point)
					}.toVector
				)).map { Some(_) } }
			case None => Success(None)
		}
	}
}
