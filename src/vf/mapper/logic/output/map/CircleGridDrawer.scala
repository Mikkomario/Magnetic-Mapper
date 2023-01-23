package vf.mapper.logic.output.map

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.range.NumericSpan
import utopia.flow.parse.AutoClose._
import utopia.genesis.graphics.{DrawSettings, StrokeSettings}
import utopia.paradigm.shape.shape2d.{Bounds, Circle, Line}
import vf.mapper.model.coordinate.CircleGrid
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
		val circleCount = (map.maxDistance * grid.circlesUntilRadius).toInt
		println(s"Draws $circleCount circles...")
		map.mapImage { _.paintedOver2 { drawer =>
			implicit val ds: DrawSettings = settings
			drawer.draw(Bounds(map.image.size.dimensions.map { len => NumericSpan(len - 12.0, len - 2.0) }))
			// Draws the items in a coordinate system that is positioned and scaled to match the map
			println("Adjusts the coordinate system...")
			drawer.translated(map.equator.north).scaled(map.equator.radius).consume { drawer =>
				(Circle.zero +: (0 until circleCount).iterator.map(grid.circle)).paired.zipWithIndex
					.foreach { case (circles, index) =>
						println(s"Drawing circle ${index + 1} / $circleCount")
						// Draws the (outer) circle
						drawer.draw(circles.second)
						// Draws the sector lines
						grid.sectorStartAnglesAt(index).foreach { angle =>
							drawer.draw(Line.lenDir(circles.map { _.radius }.toSpan, angle))
						}
					}
			}
		} }
	}
}
