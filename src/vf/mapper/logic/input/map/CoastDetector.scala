package vf.mapper.logic.input.map

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.mutable.MutableMatrix
import utopia.flow.operator.{Identity, Sign}
import utopia.flow.view.mutable.Pointer
import utopia.genesis.image.{Image, Pixels}
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Direction2D
import utopia.paradigm.shape.shape2d.Point
import vf.mapper.model.coordinate.MapPoint
import vf.mapper.model.enumeration.TerrainType.{Land, Snow, Water}
import vf.mapper.model.map.MapImage

/**
 * Used for detecting coastlines within an image map
 * @author Mikko Hilpinen
 * @since 22.1.2023, v0.1
 * @param source The image from which coastal data is taken
 */
// TODO: Refactor and clean up this class (convert into an object)
class CoastDetector(source: MapImage)
{
	// ATTRIBUTES   ----------------------
	
	lazy val terrainMatrix = source.image.pixels.map(terrainForColor)
	lazy val coastMatrix = {
		// Identifies the pixels which lie on the coast
		val mutable = MutableMatrix(terrainMatrix.mapWithIndex { (terrain, pos) =>
			Pointer(terrain != Water && terrainMatrix.viewRegionAround(pos).iterator.contains(Water))
		})
		val cropped = mutable.crop(1)
		// Cleans the pixels up
		def adjust(pos: Pair[Int], dir: Direction2D) = pos.mapSide(dir.axis.sign) { _ + dir.sign.modifier }
		// Removes coast pixels that are surrounded by coast on all othagonal sides
		cropped.updateAllWithIndex { (isCoast, pos) =>
			if (isCoast)
				Direction2D.values.exists(dir => !cropped(adjust(pos, dir)))
			else
				false
		}
		// Removes rectangular corners
		var cornerRemoved = true
		while (cornerRemoved) {
			cornerRemoved = cropped.pointers.iteratorWithIndex.exists { case (p, pos) =>
				if (p.value) {
					val region = mutable.viewRegionAround(pos.map { _ + 1 })
					val isCorner = Sign.values.existsExactCount(1) { end =>
						Sign.values.forall { axis => !region.viewLinesAlong(axis).end(end).contains(true) }
					} && region.iterator.existsCount(3)(Identity)
					if (isCorner) {
						/*
						println(s"Found corner at $pos")
						region.rowsView.foreach { row => println(row.map { if (_) "O" else "-" }.mkString) }
						println("=>")
						Sign.values.flatMap { axis =>
							val lines = region.viewLinesAlong(axis)
							Sign.values.map { end => lines.end(end) }
						}.foreach { line => println(line.map { if (_) "O" else "-" }.mkString) }*/
						p.value = false
						true
					}
					else
						false
				}
				else
					false
			}
		}
		// TODO: Clean
		mutable.currentState
	}
	// TODO: Return coast polygons instead, where additional points have been removed
	lazy val coastPoints = coastMatrix.iteratorWithIndex.flatMap { case (isCoast, pos) =>
		if (isCoast)
			Some(MapPoint.pixel(Point.from(pos.map { _.toDouble }))(source.equator))
		else
			None
	}.toVector
	
	lazy val terrainImage = Image.fromPixels(Pixels(terrainMatrix.map {
		case Water => Color.blue
		case Snow => Color.white
		case Land => Color.green
	}))
	lazy val toImage = Image.fromPixels(Pixels(coastMatrix.map { if (_) Color.black else Color.white }))
	
	
	// OTHER    --------------------------
	
	private def terrainForColor(color: Color) = {
		if (color.luminosity > 0.8)
			Snow
		else {
			val nonBlue = Pair(color.red, color.green)
			if (nonBlue.forall { _ < color.blue - 0.1 })
				Water
			else
				Land
		}
	}
}
