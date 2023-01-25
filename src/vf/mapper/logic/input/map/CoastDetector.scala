package vf.mapper.logic.input.map

import utopia.flow.collection.immutable.{Matrix, Pair}
import utopia.genesis.image.{Image, Pixels}
import utopia.paradigm.color.Color
import utopia.paradigm.shape.shape2d.Point
import vf.mapper.model.coordinate.MapPoint
import vf.mapper.model.enumeration.TerrainType
import vf.mapper.model.enumeration.TerrainType.{Land, Snow, Water}
import vf.mapper.model.map.MapImage

/**
 * Used for detecting coastlines within an image map
 * @author Mikko Hilpinen
 * @since 22.1.2023, v0.1
 */
object CoastDetector
{
	/**
	 * Reads terrain data from a map
	 * @param map Map that is being read
	 * @return A matrix containing the terrain type for each map pixel
	 */
	def detectTerrainFrom(map: MapImage) = map.image.pixels.map(terrainForColor)
	
	/**
	 * Reads coastline from a terrain matrix
	 * @param terrainMatrix A matrix that contains a terrain type for each cell
	 * @return A matrix that contains true for each cell that lies on the coast (i.e. between land and water)
	 */
	def detectCoastFrom(terrainMatrix: Matrix[TerrainType]) =
		terrainMatrix.mapWithIndex { (terrain, pos) =>
			terrain != Water && terrainMatrix.viewRegionAround(pos).iterator.contains(Water)
		}
	/**
	 * Reads coastline from a map.
	 * In case you have acquired a terrain matrix, please use that as an input parameter instead.
	 * @param map Map that is being read
	 * @return A matrix that contains true for each pixel that lies on the coast (i.e. between land and water)
	 */
	def detectCoastFrom(map: MapImage): Matrix[Boolean] = detectCoastFrom(detectTerrainFrom(map))
	
	/**
	 * Draws a terrain matrix as an image
	 * @param terrainMatrix Matrix to draw
	 * @return An image based on the the specified terrain matrix
	 */
	def drawTerrain(terrainMatrix: Matrix[TerrainType]) =
		Image.fromPixels(Pixels(terrainMatrix.map {
			case Water => Color.blue
			case Snow => Color.white
			case Land => Color.green
		}))
	/**
	 * Draws a coastline as an image
	 * @param coastMatrix Matrix to draw
	 * @return An image where the coastline is highlighted
	 */
	def drawCoast(coastMatrix: Matrix[Boolean]) =
		Image.fromPixels(Pixels(coastMatrix.map { if (_) Color.black else Color.white }))
	
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
	
	// An algorithm for cleaning coast pixels. Use if you want to create a polygon based on the coast points.
	/*{
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
		mutable.currentState
	}*/
}