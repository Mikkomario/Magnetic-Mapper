package vf.mapper.model.coordinate

import utopia.paradigm.shape.shape2d.area.Circle

/**
 * A coordinate system based on an equator on an azimuthal equidistant map
 * @author Mikko Hilpinen
 * @since 23.1.2023, v0.1
 */
case class Equator(circle: Circle)
{
	/**
	 * @return The location of the north pole
	 */
	def north = circle.origin
	/**
	 * @return Radius of the equator
	 */
	def radius = circle.radius
}