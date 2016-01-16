package akashic.storage.service

import akashic.storage.{files, Server}
import akashic.storage.patch.{Part, Commit, PatchLog}
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
      def name = "Upload Part"
      def resource = Resource.forObject(bucketName, keyName)
      def runOnce = {
        val bucket = findBucket(tree, bucketName)
        val key = findKey(bucket, keyName)
        val upload = findUpload(key, uploadId)
        // similar to ensuring the existence of key directory
        // in the PutObject operation
        Commit.once(upload.partPath(partNumber)) { patch =>
          val partPatch = patch.asPart
          partPatch.init
        }
        val part = upload.findPart(partNumber).get
        val computedMD5 = files.computeMD5(partData)
        Commit.retry(part.versions) { patch =>
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
