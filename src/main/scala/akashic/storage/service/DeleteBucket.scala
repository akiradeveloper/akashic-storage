package akashic.storage.service

import akashic.storage.server
import com.twitter.finagle.http.Request
import io.finch._

object DeleteBucket {
  val matcher = delete(string ?
    extractRequest
  ).as[t]
  val endpoint = matcher { a:t => a.run }

  case class t(bucketName: String,
               req: Request) extends Task[Output[Unit]] {
    override def name: String = "DELETE Bucket"
    override def resource = Resource.forBucket(bucketName)
    override def runOnce = {
      val bucket = findBucket(server.tree, bucketName)
      if (!bucket.listKeys.forall(_.deletable))
        failWith(Error.BucketNotEmpty())
      server.astral.free(bucket.root)
      NoContent[Unit]
        .withHeader(X_AMZ_REQUEST_ID -> requestId)
    }
  }
}
