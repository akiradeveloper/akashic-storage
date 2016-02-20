package akashic.storage.service

import akka.http.scaladsl.model.{StatusCodes, HttpRequest}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import scala.xml.NodeSeq
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._
import akashic.storage.server

object GetBucketAcl {
  val matcher =
    get &
    parameter("acl") &
    extractRequest

  val route = matcher.as(t)(_.run)

  case class t(bucketName: String,
               req: HttpRequest) extends AuthorizedAPI {
    override def name: String = "GET Bucket ACL"
    override def resource: String = Resource.forBucket(bucketName)
    override def runOnce: Route = {
      val bucket = findBucket(server.tree, bucketName)
      val bucketAcl = Acl.fromBytes(bucket.acl.read)
      val headers = ResponseHeaderList.builder
        .withHeader(X_AMZ_REQUEST_ID, requestId)
        .build
      complete(StatusCodes.OK, headers, bucketAcl.toXML)
    }
  }
}
