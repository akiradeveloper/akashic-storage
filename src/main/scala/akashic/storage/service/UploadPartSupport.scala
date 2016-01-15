package akashic.storage.service

import akashic.storage.{files, Server}
import akashic.storage.patch.{Commit, PatchLog}
import akashic.storage.service.Error.Reportable
import io.finch._
import org.apache.http.HttpHeaders

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
                 partNumber: Int,
                 partData: Array[Byte],
                 requestId: String,
                 callerId: String) extends Task[Output[Unit]] with Reportable {
      def resource = Resource.forObject(bucketName, keyName)
      def runOnce = {
        val bucket = findBucket(tree, bucketName)
        val key = findKey(bucket, keyName)
        val upload = findUpload(key, uploadId)
        val part: PatchLog = upload.part(partNumber)
        // similar to ensuring the existence of key directory
        // in the PutObject operation
        Commit.once(part.root) { patch => }
        val computedMD5 = files.computeMD5(partData)
        Commit.retry(part) { patch =>
          val dataPatch = patch.asData
          dataPatch.init

          dataPatch.writeBytes(partData)
        }
        Ok()
          .withHeader(X_AMZ_REQUEST_ID -> requestId)
          .withHeader(HttpHeaders.ETAG -> computedMD5)
      }
    }
  }
}
