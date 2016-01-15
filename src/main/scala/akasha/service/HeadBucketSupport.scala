package akasha.service

import akasha.Server
import akasha.service.Error.Reportable
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
        val bucket = tree.findBucket(bucketName) match {
          case Some(a) => a
          case None => failWith(Error.NoSuchBucket())
        }
        // TODO check acl
        Ok()
          .withHeader("x-amz-request-id" -> requestId)
      }
    }
  }
}
