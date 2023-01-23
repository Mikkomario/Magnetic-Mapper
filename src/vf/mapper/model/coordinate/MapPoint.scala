package vf.mapper.model.coordinate

import utopia.flow.collection.immutable.Pair
import utopia.paradigm.angular.{Angle, Rotation}
import utopia.paradigm.enumeration.RotationDirection.Counterclockwise
import utopia.paradigm.shape.shape2d.{Point, Vector2D}

object MapPoint
{
	/**
	 * @param vector A position from the north pole to the specified destination, where length 1 is the equator radius.
	 * @return A new map point based on the specified position.
	 */
	def apply(vector: Vector2D): MapPoint = {
		// We use counter-clockwise direction
		val latitude = Rotation.ofDegrees((1 - vector.length) * 90.0, Counterclockwise)
		// 0 direction is at "down", counter-clockwise is the positive direction here (which is a bit convoluted)
		val longitude = -vector.direction.relativeTo(Angle.down)
		apply(vector, latitude, longitude)
	}
	
	/**
	 * Converts a pixel coordinate into a map point
	 * @param pixel A pixel coordinate
	 * @param equator The equator in the image
	 * @return A map coordinate
	 */
	def pixel(pixel: Point)(implicit equator: Equator): MapPoint =
		apply((pixel - equator.north).toVector / equator.radius)
	
	/**
	 * Converts a latitude, longitude -pair into a map point
	 * @param latitude Latitude as rotation from the equator where counter-clockwise is north and clockwise is south
	 * @param longitude Longitude as an angle relative to the Greenwich, England, where positive direction is
	 *                  counter-clockwise
	 * @return A new map point
	 */
	def latLong(latitude: Rotation, longitude: Angle) = {
		val distance = latitude.counterClockwiseDegrees / 90.0
		// Longitude is subtracted because the positive direction is different between these coordinate systems
		// Counter-clockwise in longitude, clockwise in Paradigm
		val direction = Angle.down - longitude.toRotation
		apply(Vector2D.lenDir(distance, direction), latitude, longitude)
	}
	/**
	 * Converts a latitude, longitude -pair into a map point
	 * @param latitude  Latitude as degrees to north from the equator (negative is south)
	 * @param longitude Longitude as an angle relative to the Greenwich, England, where positive direction is
	 *                  west
	 * @return A new map point
	 */
	def latLongDegrees(latitude: Double, longitude: Double): MapPoint =
		latLong(Rotation.ofDegrees(latitude, Counterclockwise), Angle.ofDegrees(longitude))
}

/**
 * Represents a point on an unmodified azimuthal equidistant map
 * @author Mikko Hilpinen
 * @since 23.1.2023, v0.1
 * @param vector This point as a vector relative to the north pole, where the length of 1 lies at the equator
 * @param latitude The latitude coordinate of this position in the globe system.
 *                 0 means equator. 90 counter-clockwise means the north pole and 90 clockwise means the south rim.
 * @param longitude The longitude coordinate of this position in the globe system.
 *                  0 means Greenwich, England. Positive ]0, 180[ values are towards the east
 *                  and negative ]180, 360[ towards the west.
 */
case class MapPoint(vector: Vector2D, latitude: Rotation, longitude: Angle)
{
	/**
	 * @return The latitude and longitude coordinates of this point, as degrees
	 */
	def latLongDegrees = Pair(latitude.counterClockwiseDegrees, longitude.degrees)
	
	/**
	 * @param imageEquator The equator at the visual map
	 * @return This point as a pixel in a visual map
	 */
	def pixel(implicit imageEquator: Equator) =
		imageEquator.north + vector * imageEquator.radius
}
