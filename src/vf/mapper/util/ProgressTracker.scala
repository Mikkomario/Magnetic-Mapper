package vf.mapper.util

import utopia.flow.event.listener.ChangeListener
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.eventful.PointerWithEvents

import java.time.Instant
import scala.concurrent.duration.FiniteDuration

/**
 * Used for generating progress events
 * @author Mikko Hilpinen
 * @since 25.1.2023, v0.1
 */
class ProgressTracker(completionCount: Double, minUpdateDelay: FiniteDuration, listener: ChangeListener[Double])
{
	// ATTRIBUTES   -------------------------
	
	private val progressPointer = new PointerWithEvents[Double](0.0)
	private var lastUpdateTime = Instant.EPOCH
	private val firstSkippedPointer = Pointer.empty[Double]()
	
	
	// INITIAL CODE -------------------------
	
	progressPointer.addListener { e =>
		val t = Now.toInstant
		if (e.newValue >= 1.0 || t - lastUpdateTime >= minUpdateDelay) {
			lastUpdateTime = t
			listener.onChangeEvent(firstSkippedPointer.pop() match {
				case Some(first) => e.copy(e.values.withFirst(first))
				case None => e
			})
		}
		else
			firstSkippedPointer.value = Some(e.oldValue)
	}
	
	
	// OTHER    ----------------------------
	
	/**
	 * Advances this tracker's progress
	 * @param progress Amount of advancement [0, 1]. Default = one step.
	 */
	def proceed(progress: Double = 1.0 / completionCount) = progressPointer.update { _ + progress }
}
