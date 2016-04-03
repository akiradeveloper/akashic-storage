package akashic.storage.service

import akashic.storage.server
import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._

object HeadBucket {
  val matcher =
    head &
    extractBucket

  val route = matcher.as(t)(_.run)

  case class t(bucketName: String) extends AuthorizedAPI {
    def name = "HEAD Bucket"
    def resource = Resource.forBucket(bucketName)
    def runOnce = {
      val bucket = findBucket(server.tree, bucketName)

      val bucketAcl = bucket.acl.get
      if (!bucketAcl.grant(callerId, Acl.Read()))
        failWith(Error.AccessDenied())

      val headers = ResponseHeaderList.builder
        .withHeader(X_AMZ_REQUEST_ID, requestId)
        .build

      complete(StatusCodes.OK, headers, HttpEntity.Empty)
    }
  }
}
