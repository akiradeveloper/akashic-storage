package akashic.storage.service

import akashic.storage.server
import akashic.storage.service.Error.Reportable
import com.twitter.finagle.http.Request
import io.finch._

object HeadBucket {
  val matcher = head(string ? extractRequest).as[t]
  val endpoint = matcher { a: t => a.run }
  case class t(bucketName: String,
               req: Request) extends Task[Output[Unit]] {
    def name = "HEAD Bucket"
    def resource = Resource.forBucket(bucketName)
    def runOnce = {
      val bucket = findBucket(server.tree, bucketName)
      // TODO check acl
      Ok()
        .withHeader(X_AMZ_REQUEST_ID -> requestId)
    }
  }
}
