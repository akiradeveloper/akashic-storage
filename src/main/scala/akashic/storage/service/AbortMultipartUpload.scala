package akashic.storage.service

import akashic.storage._
import com.twitter.finagle.http.Request
import io.finch._

object AbortMultipartUpload {
  val matcher = delete(keyMatcher / paramExists("uploadId") ?
    param("uploadId") ?
    extractRequest).as[t]
  val endpoint = matcher { a: t => a.run }

  case class t(bucketName: String, keyName: String, uploadId: String,
               req: Request) extends Task[Output[Unit]] {
    override def name: String = "Abort Multipart Upload"
    override def resource = Resource.forObject(bucketName, keyName)

    override def runOnce = {
      val bucket = findBucket(server.tree, bucketName)
      val key = findKey(bucket, keyName)
      val upload = findUpload(key, uploadId)
      server.astral.dispose(upload.root)
      NoContent[Unit]
        .withHeader(X_AMZ_REQUEST_ID -> requestId)
    }
  }
}
