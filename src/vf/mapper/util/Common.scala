package vf.mapper.util

import utopia.flow.async.context.ThreadPool
import utopia.flow.util.logging.{Logger, SysErrLogger}

import scala.concurrent.ExecutionContext

/**
 * Contains commonly used values
 * @author Mikko Hilpinen
 * @since 22.1.2023, v0.1
 */
object Common
{
	implicit val log: Logger = SysErrLogger
	implicit val exc: ExecutionContext = new ThreadPool("Magnetic-Mapper").executionContext
}
