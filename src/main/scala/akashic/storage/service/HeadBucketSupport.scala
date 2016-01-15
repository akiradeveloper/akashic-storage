package akashic.storage.service

import akashic.storage.Server
import akashic.storage.service.Error.Reportable
import io.finch._

trait HeadBucketSupport {
  self: Server =>
  object HeadBucket {
    val matcher = head(string ? RequestId.reader ? CallerId.reader).as[t]
    val endpoint = matcher { a: t => a.run }
    case class t(bucketName: String,
                 requestId: String,
                 callerId: String) extends Task[Output[Unit]] with Reportable {
      def resource = Resource.forBucket(bucketName)
      def runOnce = {
        val bucket = findBucket(tree, bucketName)
        // TODO check acl
        Ok()
          .withHeader(X_AMZ_REQUEST_ID -> requestId)
      }
    }
  }
}
