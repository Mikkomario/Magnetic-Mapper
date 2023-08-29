package vf.mapper.test

import utopia.access.http.Status
import utopia.bunnymunch.jawn.JsonBunny
import utopia.disciple.apache.Gateway
import utopia.disciple.http.request.Request
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.context.ThreadPool
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}

import scala.concurrent.ExecutionContext
import scala.io.Codec

/**
 * A test application for testing access to the NGDC NOAA magnetic declination API.
 * See: https://www.ngdc.noaa.gov/geomag/CalcSurveyFin.shtml for more information
 * and https://www.ngdc.noaa.gov/geomag/calculators/magcalc.shtml#declination
 * @author Mikko Hilpinen
 * @since 22.1.2023, v0.1
 */
object AccessApiTest extends App
{
	Status.setup()
	
	implicit val logger: Logger = SysErrLogger
	implicit val exc: ExecutionContext = new ThreadPool("Magnetic-Test")
	
	val gateway = Gateway(Vector(JsonBunny), parameterEncoding = Some(Codec.UTF8), allowJsonInUriParameters = false)
	
	println("Requesting data for Lahden Urheilukeskus...")
	val response = gateway.valueResponseFor(Request("https://www.ngdc.noaa.gov/geomag-web/calculators/calculateDeclination",
		params = Model.from("key" -> "zNEw7", "resultFormat" -> "json",
			"lat1" -> "60.98398618621169", "lon1" -> "25.634853675575798"))).waitForResult(60.seconds).get
	
	println(s"Status: ${ response.status }")
	println(s"Headers:\n${ response.headers.fields.map { case (h, v) => s"- $h: $v" }.mkString("\n") }")
	println("----\nBody----")
	println(response.body)
}
