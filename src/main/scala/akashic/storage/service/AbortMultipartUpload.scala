package akashic.storage.service

import akashic.storage._
import akka.http.scaladsl.model.{HttpEntity, StatusCodes, HttpRequest}
import akka.http.scaladsl.server.Directives._

object AbortMultipartUpload {
  val matcher =
    delete &
    extractObject &
    parameter("uploadId") &
    extractRequest

  val route = matcher.as(t)(_.run)

  case class t(bucketName: String, keyName: String, uploadId: String,
               req: HttpRequest) extends AuthorizedAPI {
    override def name: String = "Abort Multipart Upload"
    override def resource = Resource.forObject(bucketName, keyName)

    override def runOnce = {
      val bucket = findBucket(server.tree, bucketName)
      val key = findKey(bucket, keyName)
      val upload = findUpload(key, uploadId)
      server.astral.free(upload.root)
      val headers = ResponseHeaderList.builder
        .withHeader(X_AMZ_REQUEST_ID, requestId)
        .build
      complete(StatusCodes.NoContent, headers, HttpEntity.Empty)
    }
  }
}
