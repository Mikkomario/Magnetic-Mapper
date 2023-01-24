package vf.mapper.logic.input.api

import utopia.access.http.Headers
import utopia.annex.controller.Api
import utopia.bunnymunch.jawn.JsonBunny
import utopia.disciple.apache.Gateway
import utopia.disciple.controller.RequestInterceptor
import utopia.disciple.http.request.{Body, StringBody}
import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.util.logging.Logger

import scala.concurrent.ExecutionContext
import scala.io.Codec

/**
 * Provides an interface for making requests to the declination API
 * @author Mikko Hilpinen
 * @since 24.1.2023, v0.1
 */
class DeclinationApi(key: String)(implicit override val log: Logger) extends Api
{
	// ATTRIBUTES  ---------------------------
	
	override protected val rootPath: String = "https://www.ngdc.noaa.gov/geomag-web"
	
	override protected lazy val gateway: Gateway = Gateway(
		Vector(JsonBunny), parameterEncoding = Some(Codec.UTF8),
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
	def getDeclinationAt(latLong: Pair[Double])(implicit exc: ExecutionContext) =
		get("calculators/calculateDeclination",
			params = Model.from("lat1" -> latLong.first, "lon1" -> latLong.second))
}
