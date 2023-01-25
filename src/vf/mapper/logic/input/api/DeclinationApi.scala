package vf.mapper.logic.input.api

import utopia.access.http.Headers
import utopia.annex.controller.Api
import utopia.bunnymunch.jawn.JsonBunny
import utopia.disciple.apache.Gateway
import utopia.disciple.controller.RequestInterceptor
import utopia.disciple.http.request.{Body, StringBody, Timeout}
import utopia.flow.async.TryFuture
import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.Logger

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.io.Codec

/**
 * Provides an interface for making requests to the declination API
 * @author Mikko Hilpinen
 * @since 24.1.2023, v0.1
 */
class DeclinationApi(key: String, timeout: FiniteDuration = 5.minutes)(implicit override val log: Logger) extends Api
{
	// ATTRIBUTES  ---------------------------
	
	override protected val rootPath: String = "https://www.ngdc.noaa.gov/geomag-web"
	
	// From The API-response:
	// key: To get the API key, register at https://www.ngdc.noaa.gov/geomag/calculators/magcalc.shtml
	// resultFormat: format of calculation results: 'html', 'csv', 'xml', 'json'
	override protected lazy val gateway: Gateway = Gateway(
		Vector(JsonBunny), maxConnectionsPerRoute = 1, maximumTimeout = Timeout.uniform(timeout),
		parameterEncoding = Some(Codec.UTF8),
		requestInterceptors = Vector(
			RequestInterceptor { _ ++ Model.from("key" -> key, "resultFormat" -> "json") }),
		allowJsonInUriParameters = false)
	
	
	// IMPLEMENTED  -------------------------
	
	override protected def headers: Headers = Headers.empty
	
	// Not expected to be called
	override protected def makeRequestBody(bodyContent: Value): Body = StringBody.json(bodyContent.toJson)
	
	
	// OTHER    -----------------------------
	
	/**
	 * Requests magnetic declination information
	 * @param latLong Targeted latitude + longitude coordinates
	 * @param exc Implicit execution context
	 * @return Future of the response received from the API
	 */
	// From the API response:
	// lat1: decimal degrees or degrees minutes seconds: -90.0 to 90.0
	// lon1: decimal degrees or degrees minutes seconds: -180.0 to 180.0
	def getDeclinationAt(latLong: Pair[Double])(implicit exc: ExecutionContext) =
	{
		// Makes sure the parameters meet the API requirements
		if (latLong.first.abs > 90)
			TryFuture.failure(new IllegalArgumentException(s"The specified latitude ${
				latLong.first } doesn't meet the API requirements, i.e. is not [-90.0, 90.0]"))
		else if (latLong.second.abs > 180)
			TryFuture.failure(new IllegalArgumentException(s"The specified longitude ${
				latLong.second } doesn't meet the API requirements, i.e. is not [-180.0, 180.0]"))
		else
			get("calculators/calculateDeclination",
				params = Model.from("lat1" -> latLong.first, "lon1" -> latLong.second))
	}
}
