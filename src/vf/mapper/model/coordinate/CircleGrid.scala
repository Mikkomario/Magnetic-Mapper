package vf.mapper.model.coordinate

import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration}
import utopia.flow.generic.model.mutable.DataType.IntType
import utopia.flow.generic.model.template.ModelConvertible
import utopia.paradigm.angular.Angle
import utopia.paradigm.shape.shape2d.{Circle, Vector2D}

object CircleGrid extends FromModelFactoryWithSchema[CircleGrid]
{
	override lazy val schema: ModelDeclaration =
		ModelDeclaration("circlesUntilEquator" -> IntType, "firstCircleSectorsCount" -> IntType)
	
	override protected def fromValidatedModel(model: Model): CircleGrid =
		apply(model("circlesUntilEquator").getInt, model("firstCircleSectorsCount").getInt)
}

/**
 * A grid which groups coordinates together on a circle.
 * The inner part of the circle is divided into n smaller circles, and each of the circles is divided into segments.
 * The number of segments increases for each layer.
 *
 * In this coordinate system, (0,0) is considered to represent the first (i.e. rightmost) sector (second coordinate)
 * of the innermost circle (first coordinate). The sectors start from right and move clockwise.
 * @author Mikko Hilpinen
 * @since 23.1.2023, v0.1
 * @param circlesUntilRadius The number of circles inside of and including the targeted circle.
 *                           E.g. If 2, the inner part of the circle will be divided into two circles.
 *                           Default = 1 = No additional inner circles.
 * @param innerCircleSectors The number of sectors within the innermost circle.
 *                           The number of circles for the outer circles is based on this value, plus the
 *                           'segmentMultiplier'.
 *                           Default = 4 = The innermost circle is divided into 4 regions.
 */
case class CircleGrid(circlesUntilRadius: Int = 1, innerCircleSectors: Int = 4) extends ModelConvertible
{
	// ATTRIBUTES   ---------------------------
	
	// d = 2Pi*r1 / Sc1
	// r1 = 1/circlesUntilRadius
	private val segmentLength = (2.0 * math.Pi) / (innerCircleSectors * circlesUntilRadius)
	
	
	// IMPLEMENTED  ---------------------------
	
	override def toModel: Model = Model.from(
		"circlesUntilEquator" -> circlesUntilRadius, "firstCircleSectorsCount" -> innerCircleSectors)
	
	
	// OTHER    -------------------------------
	
	/**
	 * @param circleIndex Index of the targeted circle
	 * @return Number of sectors on that circle
	 */
	def sectorCountAt(circleIndex: Int) = (2 * math.Pi * circleRadiusAt(circleIndex) / segmentLength).toInt
	/**
	 * @param circleIndex Index of the targeted circle
	 * @return The width of each sector on that circle, as an angle
	 */
	def sectorWidthAt(circleIndex: Int) = Angle.ofCircles(1.0 / sectorCountAt(circleIndex))
	/**
	 * @param circleIndex Index of the targeted circle
	 * @return The angles at which sectors start on that circle
	 */
	def sectorStartAnglesAt(circleIndex: Int) = {
		val w = sectorWidthAt(circleIndex)
		val n = sectorCountAt(circleIndex)
		(0 until n).map { w * _.toDouble }
	}
	
	/**
	 * @param index Targeted circle index (0-based)
	 * @return The radius of the targeted circle
	 */
	private def circleRadiusAt(index: Int) = (index + 1).toDouble / circlesUntilRadius
	/**
	 * @param index Targeted circle index
	 * @return Targeted circle
	 */
	def circle(index: Int)(implicit eq: Equator) = Circle(eq.north, circleRadiusAt(index) * eq.radius)
	
	/**
	 * Checks which grid index contains the specified map point
	 * @param point A point on a map
	 * @return The grid index that contains the specified point
	 */
	def cellIndexOf(point: MapPoint) = {
		// Finds the circle on which this point lies
		val circleIndex = (point.vector.length * circlesUntilRadius).toInt
		// Finds the sector
		val sectorIndex = (point.vector.direction.circleRatio * sectorCountAt(circleIndex)).toInt
		
		Pair(circleIndex, sectorIndex)
	}
	/**
	 * Returns the center coordinate of a grid cell
	 * @param index Grid cell index
	 * @return A point on the map that lies at the center of that cell
	 */
	def centerOf(index: Pair[Int]) = {
		// Determines the direction
		val sectorsCount = sectorCountAt(index.first)
		val angle = Angle.ofCircles((index.second + 0.5) / sectorsCount)
		val distance = (index.first + 0.5) / circlesUntilRadius
		MapPoint(Vector2D.lenDir(distance, angle))
	}
}
