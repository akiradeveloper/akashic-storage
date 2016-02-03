package akashic.storage.service

import akashic.storage.{files, server}
import akashic.storage.patch.Commit
import akashic.storage.service.Error.Reportable
import com.twitter.concurrent.AsyncStream
import com.twitter.finagle.http.Request
import com.twitter.io.Buf
import io.finch._
import com.google.common.net.HttpHeaders._

object UploadPart {
  val matcher = put(keyMatcher / paramExists("uploadId") / paramExists("partNumber") ?
    param("uploadId") ?
    param("partNumber").as[Int] ?
    asyncBody ?
    extractRequest)
  val endpoint = matcher {
    (bucketName: String, keyName: String,
     uploadId: String,
     partNumber: Int,
     partData: AsyncStream[Buf],
     req: Request) => for {
      pd <- mkByteArray(partData)
    } yield t(bucketName, keyName, uploadId, partNumber, pd, req).run
  }

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
      Commit.replaceData(part.unwrap) { patch =>
        val dataPatch = patch.asData
        dataPatch.write(partData)
      }

      Ok()
        .withHeader(X_AMZ_REQUEST_ID -> requestId)
        .withHeader(ETAG -> quoteString(computedMD5))
    }
  }
}
