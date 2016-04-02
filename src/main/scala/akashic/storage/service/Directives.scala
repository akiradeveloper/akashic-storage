package akashic.storage.service

import akashic.storage.auth.GetCallerId
import akashic.storage.service.Acl.Grant
import akashic.storage.{HeaderList, auth}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive, Directive0, Directive1}

import scala.collection.immutable

trait Directives {

  def withParameter(name: String): Directive0 = parameter(name).tflatMap(a => pass)

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

  val extractMetadata: Directive1[HeaderList] =
    extractRequest.map(a => a.headers
         .filter(_.name.startsWith("x-amz-meta-"))
         .map(a => (a.name, a.value))
         .foldLeft(HeaderList.builder) { case (acc, (k, v)) => acc.append(k, v) }
         .build
      )

  val extractBucket: Directive1[String] = path(Segment ~ (Slash | PathEnd))

  val extractObject: Directive[(String, String)] = path(Segment / Rest).tmap {
    case (bucketName: String, keyName: String) => (bucketName, encodeKeyName(keyName))
  }

  private val extractGrantHeaders: Directive1[immutable.Seq[Acl.GrantHeader]] =
    extractRequest.map(a => a.headers
         .filter(_.name.startsWith("x-amz-grant-"))
         .map(a => Acl.GrantHeader.parseLine(a.name, a.value))
      )
  val extractGrantsFromHeaders: Directive1[immutable.Seq[Grant]] =
    extractGrantHeaders.map(_.map(_.makeGrants).flatten)

  def authorizeS3v2(resource: String, requestId: String): Directive1[String] =
    extractRequest.map { req =>
      val authKey = auth.authorize(resource, req)
      GetCallerId(authKey, requestId, resource)()
    }
}
