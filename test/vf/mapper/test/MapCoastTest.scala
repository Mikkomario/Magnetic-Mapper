package vf.mapper.test

import utopia.flow.parse.file.FileExtensions._
import vf.mapper.logic.input.map.CoastDetector
import vf.mapper.test.TestValues._

/**
 * A test for drawing the coast line
 * @author Mikko Hilpinen
 * @since 22.1.2023, v0.1
 */
object MapCoastTest extends App
{
	val terrain = CoastDetector.detectTerrainFrom(map)
	println("Drawing terrain...")
	CoastDetector.drawTerrain(terrain).writeToFile(mapOutputDir/"terrain.png")
	println("Drawing coast...")
	CoastDetector.drawCoast(CoastDetector.detectCoastFrom(terrain)).writeToFile(mapOutputDir/"coasts.png")
	println("Done!")
	mapOutputDir.openInDesktop()
}
