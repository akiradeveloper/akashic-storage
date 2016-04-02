package akashic.storage

import java.net.{URLDecoder, URLEncoder}
import java.util.concurrent.TimeUnit

import akashic.storage.admin._
import akashic.storage.auth.GetCallerId
import akashic.storage.service.Acl.Grant
import akka.http.scaladsl.model.Multipart
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.DebuggingDirectives
import akka.http.scaladsl.server.{Directive0, Directive, Directive1}
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, FiniteDuration}

package object service {
  // first appearance wins
  implicit class _Option[A](unwrap: Option[A]) {
    def `<+`(other: Option[A]): Option[A] = 
      unwrap match {
        case Some(a) => unwrap
        case None => other
      }
  }

  def withParameter(name: String): Directive0 = parameter(name).tflatMap(a => pass)

  private val extractGrantHeaders: Directive1[immutable.Seq[Acl.GrantHeader]] =
    extractRequest.map(a => a.headers
         .filter(_.name.startsWith("x-amz-grant-"))
         .map(a => Acl.GrantHeader.parseLine(a.name, a.value))
      )
  val extractGrantsFromHeaders: Directive1[immutable.Seq[Grant]] =
    extractGrantHeaders.map(_.map(_.makeGrants).flatten)

  val optionalBinaryBody: Directive1[Option[Array[Byte]]] = entity(as[Array[Byte]]).map { a =>
    if (a.size == 0) {
      None
    } else {
      Some(a)
    }
  }

  val optionalStringBody: Directive1[Option[String]] = entity(as[String]).map { a =>
    a match {
      case "" => None
      case a => Some(a)
    }
  }

  val extractMetadata: Directive1[HeaderList.t] =
    extractRequest.map(a => a.headers
         .filter(_.name.startsWith("x-amz-meta-"))
         .map(a => (a.name, a.value))
         .foldLeft(HeaderList.builder) { case (acc, (k, v)) => acc.append(k, v) }
         .build
      )

  val X_AMZ_REQUEST_ID = "x-amz-request-id"
  val X_AMZ_VERSION_ID = "x-amz-version-id"
  val X_AMZ_DELETE_MARKER = "x-amz-delete-marker"

  def quoteString(raw: String): String = s""""${raw}""""

  def encodeKeyName(keyName: String): String = URLEncoder.encode(keyName, "UTF-8")
  def decodeKeyName(keyName: String): String = URLDecoder.decode(keyName, "UTF-8")
  val extractBucket: Directive1[String] = path(Segment ~ (Slash | PathEnd))
  val extractObject: Directive[(String, String)] = path(Segment / Rest).tmap {
    case (bucketName: String, keyName: String) => (bucketName, encodeKeyName(keyName))
  }

  trait AuthorizableTask extends Task with Error.Reportable with Authorizable with RequestIdAllocable with Measure
  type AuthorizedAPI = AuthorizableTask

  trait AnonymousTask extends Task with Error.Reportable with RequestIdAllocable with Measure
  type AnonymousAPI = AnonymousTask

  val logger = Logger(LoggerFactory.getLogger("akashic.storage.service"))
  val apiLogger = withLog(logger).tflatMap(_ => DebuggingDirectives.logRequestResult(""))

  def authorizeS3v2(resource: String, requestId: String): Directive1[String] =
    extractRequest.map { req =>
      val authKey = auth.authorize(resource, req)
      GetCallerId(authKey, requestId, resource)()
    }
}
