package vf.mapper.logic.output.map

import utopia.flow.collection.CollectionExtensions._
import utopia.genesis.graphics.{DrawSettings, StrokeSettings}
import utopia.paradigm.shape.shape2d.area.Circle
import utopia.paradigm.shape.shape2d.line.Line
import vf.mapper.model.coordinate.{CircleGrid, Equator}
import vf.mapper.model.map.MapImage

/**
 * Used for drawing circle grids on top of maps
 * @author Mikko Hilpinen
 * @since 23.1.2023, v0.1
 */
object CircleGridDrawer
{
	/**
	 * Overlays a grid system over a map
	 * @param map Map to modify
	 * @param grid Grid to overlay on top of the specified map
	 * @param settings Stroke draw settings
	 * @return A modified copy of the map, containing the overlaid grid
	 */
	def overlay(map: MapImage, grid: CircleGrid)(implicit settings: StrokeSettings) = {
		implicit val eq: Equator = map.equator
		val circleCount = (map.maxDistance * grid.circlesUntilRadius).toInt
		map.mapImage { _.paintedOver { drawer =>
			implicit val ds: DrawSettings = settings
			// Draws the items in a coordinate system that is positioned and scaled to match the map
			drawer.use { drawer =>
				(Circle(eq.north, 0) +: (0 until circleCount).iterator.map(grid.circle)).paired.zipWithIndex
					.foreach { case (circles, index) =>
						// Draws the (outer) circle
						drawer.draw(circles.second)
						// Draws the sector lines
						grid.sectorStartAnglesAt(index).foreach { angle =>
							drawer.draw(Line.lenDir(circles.map { _.radius }.toSpan, angle).translated(eq.north))
						}
					}
			}
		} }
	}
}
