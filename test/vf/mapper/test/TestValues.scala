package vf.mapper.test

import utopia.flow.parse.file.FileExtensions._
import utopia.genesis.image.Image
import utopia.paradigm.shape.shape2d.Circle
import vf.mapper.model.coordinate.Equator
import vf.mapper.model.map.MapImage

import java.nio.file.{Path, Paths}

/**
 * Common access point for test values and settings
 * @author Mikko Hilpinen
 * @since 23.1.2023, v0.1
 */
object TestValues
{
	// ATTRIBUTES   ---------------------
	
	lazy val declinationsDir: Path = Paths.get("data/input/declinations").asExistingDirectory.get
	lazy val mapOutputDir: Path = Paths.get("data/output/maps").asExistingDirectory.get
	lazy val map = {
		val image = Image.readFrom("data/input/maps/azimutal.jpg")
			.get// .withMaxSourceResolution(Size.square(480)).withScaling(1.0)
		MapImage(image, Equator(Circle(image.bounds.center, image.size.minDimension / 4.0)))
	}
}
