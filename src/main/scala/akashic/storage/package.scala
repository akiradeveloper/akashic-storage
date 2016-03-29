package akashic

import akashic.storage.backend.BAL
import akashic.storage.service._
import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.{StrictLogging, Logger}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext

package object storage {
  implicit val system: ActorSystem = ActorSystem("akashic-storage")
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher
  sys.addShutdownHook(system.shutdown)

  var server: Server = _
  implicit var fs: BAL = _

  val logger = Logger(LoggerFactory.getLogger("akashic.storage"))

  implicit class ToLoggingAdapter(log: Logger) extends LoggingAdapter {
    override def isErrorEnabled: Boolean = true
    override def isInfoEnabled: Boolean = true
    override def isDebugEnabled: Boolean = true
    override def isWarningEnabled: Boolean = true
    override protected def notifyInfo(message: String): Unit = log.info(message)
    override protected def notifyError(message: String): Unit = log.error(message)
    override protected def notifyError(cause: Throwable, message: String): Unit = log.error(message, cause)
    override protected def notifyWarning(message: String): Unit = log.warn(message)
    override protected def notifyDebug(message: String): Unit = log.debug(message)
  }
}
