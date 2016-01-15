package akasha.service

import akasha.Server
import io.finch._
import akasha.service.Error.Reportable

trait UploadPartSupport {
  self: Server =>
  object UploadPart {
    val matcher = put(string / string ?
      param("uploadId") ?
      param("partNumber").as[Int] ?
      binaryBody ?
      RequestId.reader ? CallerId.reader).as[t]
    val endpoint = matcher { a: t => a.run }
    case class t(bucketName: String, keyName: String,
                 uploadId: String,
                 partId: Int,
                 data: Array[Byte],
                 requestId: String,
                 callerId: String) extends Task[Output[Unit]] with Reportable {
      def resource = Resource.forObject(bucketName, keyName)
      def runOnce = {
        val bucket = findBucket(tree, bucketName)
        val key = findKey(bucket, keyName)
        Ok()
      }
    }
  }
}
