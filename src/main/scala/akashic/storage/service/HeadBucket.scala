package akashic.storage.service

import akashic.storage.server
import akka.http.scaladsl.model.{HttpRequest, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._

object HeadBucket {
  val matcher =
    head &
    extractBucket &
    extractRequest

  val route = matcher.as(t)(_.run)

  case class t(bucketName: String,
               req: HttpRequest) extends AuthorizedAPI {
    def name = "HEAD Bucket"
    def resource = Resource.forBucket(bucketName)
    def runOnce = {
      val bucket = findBucket(server.tree, bucketName)
      // TODO check acl

      val headers = ResponseHeaderList.builder
        .withHeader(X_AMZ_REQUEST_ID, requestId)
        .build

      complete(StatusCodes.OK, headers, HttpEntity.Empty)
    }
  }
}
