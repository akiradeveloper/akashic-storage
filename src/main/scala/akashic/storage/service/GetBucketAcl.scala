package akashic.storage.service

import akashic.storage.server
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

object GetBucketAcl {
  val matcher =
    get &
    extractBucket &
    withParameter("acl")

  val route = matcher.as(t)(_.run)

  case class t(bucketName: String) extends AuthorizedAPI {
    override def name: String = "GET Bucket ACL"
    override def resource: String = Resource.forBucket(bucketName)
    override def runOnce: Route = {
      val bucket = findBucket(server.tree, bucketName)
      val bucketAcl = bucket.acl.get
      val headers = ResponseHeaderList.builder
        .withHeader(X_AMZ_REQUEST_ID, requestId)
        .build

      complete(StatusCodes.OK, headers, bucketAcl.toXML)
    }
  }
}
