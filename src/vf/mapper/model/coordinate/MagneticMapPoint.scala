package vf.mapper.model.coordinate

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Constant, Model}
import utopia.flow.generic.model.template.{ModelConvertible, ModelLike, Property}
import utopia.flow.view.template.Extender
import utopia.paradigm.angular.Rotation
import utopia.paradigm.generic.ParadigmValue._
import utopia.paradigm.shape.shape2d.Point

import scala.util.Try

object MagneticMapPoint extends FromModelFactory[MagneticMapPoint]
{
	override def apply(model: ModelLike[Property]): Try[MagneticMapPoint] =
		MapPoint(model).map { point => apply(point, model("declination").getRotation) }
}

/**
 * Attaches magnetic declination information to a coordinate
 * @author Mikko Hilpinen
 * @since 24.1.2023, v0.1
 * @param point Wrapped map point
 * @param declination Magnetic declination. I.e. How much the magnetic north is rotated from the Polaris / true north.
 *                    Clockwise direction is called east and counter-clockwise is called west.
 *                    This is because of how they appear on a compass.
 */
case class MagneticMapPoint(point: MapPoint, declination: Rotation) extends Extender[MapPoint] with ModelConvertible
{
	// ATTRIBUTES   ---------------------------
	
	/**
	 * This location, when magnetic declination has been corrected.
	 * Meaning that, from this (corrected) position,
	 * when following the compass heading of this (original) location to the north,
	 * one would travel north and eventually arrive at the north pole (0,0).
	 */
	lazy val corrected = point.copy(vector = point.vector.rotatedAround(declination, Point.origin))
	
	
	// IMPLEMENTED  ---------------------------
	
	override def wrapped: MapPoint = point
	
	override def toModel: Model = point.toModel + Constant("declination", declination)
}
