package vf.mapper.model.map

import utopia.genesis.image.Image
import vf.mapper.model.coordinate.Equator

/**
 * Represents a visual map with a coordinate system
 * @author Mikko Hilpinen
 * @since 23.1.2023, v0.1
 * @param image This map as an image
 * @param equator The size and position of the equator on this map
 */
case class MapImage(image: Image, equator: Equator)
{
	// ATTRIBUTES   -----------------------
	
	/**
	 * The maximum distance that is fully visible on this map, relative to the number of equator radi.
	 * E.g. 1.0 would mean that only the norther hemiplane is fully visible.
	 */
	lazy val maxDistance =
		image.size.mergeWith(equator.north) { (len, origin) => origin min (len - origin) }.minDimension / equator.radius
	
	
	// OTHER    ---------------------------
	
	/**
	 * Maps the image of this map. Expects the equator's position and size not to be affected.
	 * @param f A mapping function for this map's image
	 * @return A mapped copy of this map
	 */
	def mapImage(f: Image => Image) = copy(f(image))
}
