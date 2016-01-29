package akashic.storage.service

import akashic.storage.{files, server}
import akashic.storage.patch.Commit
import akashic.storage.service.Error.Reportable
import com.twitter.finagle.http.Request
import io.finch._
import com.google.common.net.HttpHeaders._

object UploadPart {
  val matcher = put(keyMatcher / paramExists("uploadId") / paramExists("partNumber") ?
    param("uploadId") ?
    param("partNumber").as[Int] ?
    binaryBody ?
    extractRequest).as[t]
  val endpoint = matcher { a: t => a.run }
  case class t(bucketName: String, keyName: String,
               uploadId: String,
               partNumber: Int,
               partData: Array[Byte],
               req: Request) extends Task[Output[Unit]] {
    def name = "Upload Part"
    def resource = Resource.forObject(bucketName, keyName)
    def runOnce = {
      val bucket = findBucket(server.tree, bucketName)
      val key = findKey(bucket, keyName)
      val upload = findUpload(key, uploadId)

      val computedMD5 = files.computeMD5(partData)
      val part = upload.part(partNumber)
      Commit.replace(part.unwrap) { patch =>
        val dataPatch = patch.asData
        dataPatch.writeBytes(partData)
      }

      Ok()
        .withHeader(X_AMZ_REQUEST_ID -> requestId)
        .withHeader(ETAG -> quoteString(computedMD5))
    }
  }
}
