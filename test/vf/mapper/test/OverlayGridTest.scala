package vf.mapper.test

import utopia.flow.parse.file.FileExtensions._
import utopia.genesis.graphics.StrokeSettings
import utopia.paradigm.color.Color
import vf.mapper.logic.output.map.CircleGridDrawer
import vf.mapper.model.coordinate.CircleGrid
import vf.mapper.test.TestValues._

/**
 * A test for drawing the coast line
 * @author Mikko Hilpinen
 * @since 22.1.2023, v0.1
 */
object OverlayGridTest extends App
{
	val grids = Vector(
		CircleGrid(),
		CircleGrid(2),
		CircleGrid(4),
		CircleGrid(2, 8),
		CircleGrid(4, 8),
		CircleGrid(2, 4, 3),
		CircleGrid(4, 4, 1.5),
		CircleGrid(4, 8, 3)
	)
	
	implicit val ss: StrokeSettings = StrokeSettings(Color.red, 3.0)
	grids.foreach { grid =>
		val name = s"grid-${grid.circlesUntilRadius}-${grid.innerCircleSectors}-${grid.segmentMultiplier}"
		println(s"Drawing $name...")
		CircleGridDrawer.overlay(map, grid).image.writeToFile(mapOutputDir/s"$name.png")
	}
	
	println("Done!")
	mapOutputDir.openInDesktop()
}
