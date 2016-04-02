package akashic.storage.service

import akashic.storage.server
import akka.http.scaladsl.model.{HttpEntity, StatusCodes, HttpRequest}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

object DeleteBucket {
  val matcher =
    delete &
    extractBucket
  val route = matcher.as(t)(_.run)

  case class t(bucketName: String) extends AuthorizedAPI {
    override def name: String = "DELETE Bucket"
    override def resource = Resource.forBucket(bucketName)
    override def runOnce = {
      val bucket = findBucket(server.tree, bucketName)
      val bucketAcl = bucket.acl.get
      if (callerId != bucketAcl.owner)
        failWith(Error.AccessDenied())
      if (!bucket.listKeys.forall(_.deletable))
        failWith(Error.BucketNotEmpty())
      server.astral.free(bucket.root)
      val headers = ResponseHeaderList.builder
        .withHeader(X_AMZ_REQUEST_ID, requestId)
        .build
      complete(StatusCodes.NoContent, headers, HttpEntity.Empty)
    }
  }
}
