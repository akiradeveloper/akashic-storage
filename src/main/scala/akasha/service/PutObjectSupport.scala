package akasha.service

import akasha.Server
import akasha.service.Error.Reportable
import io.finch._

trait PutObjectSupport {
  self: Server =>
  object PutObject {
    val matcher = put(string / string ? binaryBody ? RequestId.reader ? CallerId.reader).as[t]
    val endpoint = matcher { a: t => a.run }
    case class t(bucketName: String,
                 keyName: String,
                 data: Array[Byte],
                 requestId: String,
                 callerId: String) extends Task[Output[Unit]] with Reportable {
      def resource = bucketName + "/" + keyName
      def runOnce = {
        val bucket = tree.findBucket(bucketName) match {
          case Some(a) => a
          case None => failWith(Error.NoSuchBucket())
        }
        bucket.keyPath(keyName)
        Ok()
      }
    }
  }
}
