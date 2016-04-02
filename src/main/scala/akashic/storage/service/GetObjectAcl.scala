package akashic.storage.service

import akashic.storage.server
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

object GetObjectAcl {
  val matcher =
    get &
    extractObject &
    withParameter("acl")

  val route = matcher.as(t)(_.run)

  case class t(bucketName: String, keyName: String) extends AuthorizedAPI {
    override def name: String = "GET Object ACL"
    override def resource: String = Resource.forObject(bucketName, keyName)
    override def runOnce: Route = {
      val bucket = findBucket(server.tree, bucketName)
      val key = findKey(bucket, keyName)
      val version = key.findLatestVersion match {
        case Some(a) => a
        case None => failWith(Error.NoSuchKey())
      }
      val versionAcl = version.acl.get
      if (!versionAcl.grant(callerId, Acl.ReadAcp()))
        failWith(Error.AccessDenied())

      val headers = ResponseHeaderList.builder
        .withHeader(X_AMZ_REQUEST_ID, requestId)
        .build

      complete(StatusCodes.OK, headers, versionAcl.toXML)
    }
  }
}
