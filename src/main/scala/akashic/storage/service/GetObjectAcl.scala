package akashic.storage.service

import akka.http.scaladsl.model.{StatusCodes, HttpRequest}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import scala.xml.NodeSeq
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._
import akashic.storage.server

object GetObjectAcl {
  val matcher =
    get &
    extractObject &
    withParameter("acl") &
    extractRequest

  val route = matcher.as(t)(_.run)

  case class t(bucketName: String, keyName: String,
               req: HttpRequest) extends AuthorizedAPI {
    override def name: String = "GET Object ACL"
    override def resource: String = Resource.forObject(bucketName, keyName)
    override def runOnce: Route = {
      val bucket = findBucket(server.tree, bucketName)
      val key = findKey(bucket, keyName)
      val version = key.findLatestVersion match {
        case Some(a) => a
        case None => failWith(Error.NoSuchKey())
      }
      val versionAcl = Acl.fromBytes(version.acl.read)
      if (!versionAcl.getPermission(callerId).contains(Acl.ReadAcp()))
        failWith(Error.AccessDenied())

      val headers = ResponseHeaderList.builder
        .withHeader(X_AMZ_REQUEST_ID, requestId)
        .build

      complete(StatusCodes.OK, headers, versionAcl.toXML)
    }
  }
}
