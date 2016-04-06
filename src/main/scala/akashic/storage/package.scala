package akashic

import java.io.{ByteArrayInputStream, InputStream}
import java.util.concurrent.TimeUnit

import akashic.storage.backend.BAL
import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.unmarshalling._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.StreamConverters
import akka.util.ByteString
import com.sun.xml.internal.messaging.saaj.util.ByteInputStream
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

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
  // val entityAsInputStream: Directive1[InputStream] = extractRequest.map(_.entity.withoutSizeLimit.dataBytes.runWith(StreamConverters.asInputStream(FiniteDuration(1, TimeUnit.SECONDS))))
  val entityAsInputStream = entity(as[Array[Byte]]).map(new ByteArrayInputStream(_))

  // experimental
  implicit def chunkedStreamUnmarshaller: FromRequestUnmarshaller[Stream[Array[Byte]]] =
    Unmarshaller.withMaterializer(_ => implicit mat => {
      case req ⇒ req.entity.dataBytes.runFold(Stream.empty[Array[Byte]])(_ :+ _.toArray)
    })
}
