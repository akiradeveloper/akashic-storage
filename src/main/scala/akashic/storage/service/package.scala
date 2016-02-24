package akashic.storage

import java.net.{URLEncoder, URLDecoder}
import java.util.concurrent.TimeUnit

import akashic.storage.patch.Version
import akashic.storage.service.Acl.Grant
import akka.actor.ActorSystem
import akka.http.scaladsl.model.Multipart
import akka.http.scaladsl.server.util.ConstructFromTuple
import akka.stream.{Materializer, ActorMaterializer}
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.xml.NodeSeq
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive1, Directive, Route}
import scala.collection.immutable

package object service {

  // shared actor system and materializer
  implicit var system: ActorSystem = _
  implicit var mat: ActorMaterializer = _

  // first appearance wins
  implicit class _Option[A](unwrap: Option[A]) {
    def `<+`(other: Option[A]): Option[A] = 
      unwrap match {
        case Some(a) => unwrap
        case None => other
      }
  }

  def withParameter(name: String) = parameter(name).tflatMap(a => pass)

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

  // torima impl
  def extractMetadataFromFields: Directive1[HeaderList.t] =
    entity(as[Multipart.FormData]).map { a =>
      val fut = a.toStrict(FiniteDuration.apply(30, TimeUnit.SECONDS))
      val strict = Await.result(fut, Duration.apply(30, TimeUnit.SECONDS))
      strict.strictParts
        .map(a => (a.name, a.entity.getData().decodeString("UTF-8")))
        .filter { case (k, _) => k.startsWith("x-amz-meta-") }
        .foldLeft(HeaderList.builder) { case (acc, (k, v)) => acc.append(k, v) }
        .build
    }

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

  trait AuthorizableTask[T] extends Task[T] with RequestIdAllocable[T] with Error.Reportable with Authorizable[T] with Measure[T]
  type AuthorizedAPI = AuthorizableTask[Route]

  trait AnonymousTask[T] extends Task[T] with RequestIdAllocable[T] with Error.Reportable with Measure[T]
  type AnonymousAPI = AnonymousTask[Route]
}
