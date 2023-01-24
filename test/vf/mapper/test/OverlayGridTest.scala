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
		CircleGrid(6),
		CircleGrid(8),
		CircleGrid(10),
		CircleGrid(2, 8),
		CircleGrid(4, 8),
		CircleGrid(6, 8),
		CircleGrid(8, 8),
		CircleGrid(10, 8)
	)
	
	implicit val ss: StrokeSettings = StrokeSettings(Color.red.withAlpha(0.8))
	grids.foreach { grid =>
		val name = s"grid-${grid.circlesUntilRadius}-${grid.innerCircleSectors}"
		println(s"Drawing $name...")
		CircleGridDrawer.overlay(map, grid).image.writeToFile(mapOutputDir/s"$name.png")
	}
	
	println("Done!")
	mapOutputDir.openInDesktop()
}
