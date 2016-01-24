package akashic.storage.service

import akashic.storage.compactor.PartCompactor
import akashic.storage.{files, server}
import akashic.storage.patch.{Part, Commit, PatchLog}
import akashic.storage.service.Error.Reportable
import com.twitter.finagle.http.Request
import io.finch._
import com.google.common.net.HttpHeaders._

object UploadPart {
  val matcher = put(string / string / paramExists("uploadId") / paramExists("partNumber") ?
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

      server.compactorQueue.queue(PartCompactor(part))

      Ok()
        .withHeader(X_AMZ_REQUEST_ID -> requestId)
        .withHeader(ETAG -> quoteString(computedMD5))
    }
  }
}
