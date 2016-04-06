package akashic.storage

import java.net.{URLDecoder, URLEncoder}

import akashic.storage.admin._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.DebuggingDirectives
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

package object service extends Directives with Syntax {
  // first appearance wins
  implicit class _Option[A](unwrap: Option[A]) {
    def `<+`(other: Option[A]): Option[A] = 
      unwrap match {
        case Some(a) => unwrap
        case None => other
      }
  }

  val X_AMZ_REQUEST_ID = "x-amz-request-id"
  val X_AMZ_VERSION_ID = "x-amz-version-id"
  val X_AMZ_DELETE_MARKER = "x-amz-delete-marker"

  def quoteString(raw: String): String = s""""${raw}""""

  def encodeKeyName(keyName: String): String = URLEncoder.encode(keyName, "UTF-8")
  def decodeKeyName(keyName: String): String = URLDecoder.decode(keyName, "UTF-8")

  trait AuthorizableTask extends Task with Error.Reportable with Authorizable with RequestIdAllocable with Measure
  type AuthorizedAPI = AuthorizableTask

  trait AnonymousTask extends Task with Error.Reportable with RequestIdAllocable with Measure
  type AnonymousAPI = AnonymousTask

  val logger = Logger(LoggerFactory.getLogger("akashic.storage.service"))
  val apiLogger = withLog(logger).tflatMap(_ => DebuggingDirectives.logRequestResult(""))
}
