package vf.mapper.test

import utopia.flow.async.AsyncExtensions._
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.time.TimeExtensions._
import utopia.paradigm.generic.ParadigmDataType
import vf.mapper.logic.input.map.CoastDetector
import TestValues._
import utopia.access.http.Status
import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeEvent
import utopia.genesis.graphics.{DrawSettings, StrokeSettings}
import utopia.genesis.image.Image
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.RotationDirection
import utopia.paradigm.enumeration.RotationDirection.{Clockwise, Counterclockwise}
import utopia.paradigm.shape.shape2d.{Bounds, Circle, Line, Point}
import vf.mapper.logic.input.api.{DeclinationApi, DeclinationReader}
import vf.mapper.model.coordinate.{CircleGrid, Equator, MapPoint}
import vf.mapper.util.Common._
import vf.mapper.util.ProgressTracker

import scala.util.{Failure, Success}

/**
 * Tests forming and drawing a magnetically corrected world map
 * @author Mikko Hilpinen
 * @since 25.1.2023, v0.1
 */
object CorrectedMapTest extends App
{
	ParadigmDataType.setup()
	Status.setup()
	
	implicit val equator: Equator = map.equator
	val requestTimeout = 2.minutes
	val progressReporter = ChangeListener { e: ChangeEvent[Double] =>
		println(s"${ (e.newValue * 100).toInt }%")
	}
	
	// Detects the coast from the map
	println("Detects the coast-line from the input map...")
	val coastPoints = CoastDetector.detectCoastFrom(map).iteratorWithIndex.flatMap { case (isCoast, pos) =>
		if (isCoast) Some(MapPoint.pixel(pos)) else None
	}.toVector
	println(s"Identified ${ coastPoints.size } coastal points")
	
	// Determines the magnetic declination of each coastal point
	println("\nSets up the declination reader...")
	val declinationReader = DeclinationReader
		.initialize(new DeclinationApi("zNEw7"), CircleGrid(10, 8), declinationsDir)
	println(s"Calculates the declination of all ${ coastPoints.size } points...")
	val magnetizationTracker = new ProgressTracker(coastPoints.size, 1.seconds, progressReporter)
	val magnetizedCoast = coastPoints.tryMap { p =>
		val res = declinationReader.magnetize(p).waitForResult(requestTimeout)
		magnetizationTracker.proceed()
		res
	}
	if (magnetizedCoast.isSuccess)
		println("All declinations calculated!")
	println("Saves declination status...")
	declinationReader.saveStatus().failure.foreach { log(_, "Failed to save declination status") }
	println("Status saved")
	
	magnetizedCoast match {
		case Success(coast) =>
			// Draws maps based on the acquired data
			val imgSize = map.image.size
			val whiteFrame = Image.paint2(imgSize) { drawer =>
				drawer.draw(Bounds(Point.origin, imgSize))(DrawSettings.onlyFill(Color.white))
			}
			val directionColor = Map[RotationDirection, Color](Clockwise -> Color.red, Counterclockwise -> Color.blue)
			
			// 1: The original coastline
			whiteFrame.paintedOver2 { drawer =>
				implicit val ds: DrawSettings = StrokeSettings.default
				coastPoints.foreach { p =>
					val pixel = p.pixel
					drawer.draw(Line(pixel, pixel))
				}
			}.writeToFile(mapOutputDir/"coast.png")
			
			// 2: Declination-corrected coastline
			println("Draws maps (1/3)...")
			val correctedCoastImage = Image.paint2(imgSize) { drawer =>
				implicit val ds: DrawSettings = StrokeSettings.default
				coast.foreach { point =>
					val pixel = point.corrected.pixel
					drawer.draw(Circle(pixel, 1))
				}
			}
			whiteFrame.withOverlay(correctedCoastImage).writeToFile(mapOutputDir/"corrected-coast.png")
			
			// 3: Declination-corrected coastline and the original coastline
			println("Draws maps (2/3)...")
			whiteFrame.paintedOver2 { drawer =>
				implicit val ds: DrawSettings = StrokeSettings(Color.black.withAlpha(0.5))
				coastPoints.foreach { p =>
					val pixel = p.pixel
					drawer.draw(Line(pixel, pixel))
				}
				correctedCoastImage.drawWith2(drawer)
			}.writeToFile(mapOutputDir/"corrected-and-old-coast.png")
			
			// 4: Corrected coast-line with colouring based on movement
			println("Draws maps (3/3)...")
			whiteFrame.paintedOver2 { drawer =>
				val maxShiftDistance = coast.iterator.map { p => (p.corrected.vector - p.vector).length }.max
				
				coast.foreach { p =>
					drawer.draw(Circle(p.corrected.pixel, 1))(DrawSettings.onlyFill(
						directionColor(p.declination.direction)
							.withSaturation((p.corrected.vector - p.vector).length / maxShiftDistance)))
				}
			}.writeToFile(mapOutputDir/"corrected-declination-colored.png")
			
			// 5: Original coastline with magnetic declination -highlighting
			whiteFrame.paintedOver2 { drawer =>
				val maxDeclination = coast.iterator.map { _.declination.radians }.max
				coast.foreach { p =>
					val pixel = p.pixel
					drawer.draw(Line(pixel, pixel))(StrokeSettings(directionColor(p.declination.direction)
						.withSaturation(p.declination.radians / maxDeclination)))
				}
			}.writeToFile(mapOutputDir/"magnetic-coast.png")
			
			println("All maps drawn. Process completed!")
			mapOutputDir.openInDesktop()
			
		case Failure(error) => log(error, "Failed to apply declination data")
	}
}
