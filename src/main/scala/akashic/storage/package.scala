package akashic

import akashic.storage.backend.BAL
import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.unmarshalling._
import akka.stream.ActorMaterializer
import akka.util.ByteString
import com.typesafe.scalalogging.Logger
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

  // experimental
  implicit def chunkedStreamUnmarshaller: FromRequestUnmarshaller[Stream[Array[Byte]]] =
    Unmarshaller.withMaterializer(_ => implicit mat => {
      case req ⇒ req.entity.dataBytes.runFold(Stream.empty[Array[Byte]])(_ :+ _.toArray)
    })
}
