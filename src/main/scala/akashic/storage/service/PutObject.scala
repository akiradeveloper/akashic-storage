package akashic.storage.service

import akashic.storage.compactor.KeyCompactor
import akashic.storage.patch.Commit
import akashic.storage.{HeaderList, files, server}
import com.twitter.finagle.http.Request
import com.google.common.net.HttpHeaders._
import io.finch._

object PutObject {
  val matcher = put(
    keyMatcher ?
    binaryBody ?
    headerOption("Content-Type") ?
    headerOption("Content-Disposition") ?
    extractRequest).as[t]
  val endpoint = matcher { a: t => a.run }
  case class t(bucketName: String, keyName: String,
               objectData: Array[Byte],
               contentType: Option[String],
               contentDisposition: Option[String],
               req: Request) extends Task[Output[Unit]] {
    def name = "PUT Object"
    def resource = Resource.forObject(bucketName, keyName)
    def runOnce = {
      val computedETag = files.computeMD5(objectData)
      val bucket = findBucket(server.tree, bucketName)
      Commit.once(bucket.keyPath(keyName)) { patch => 
        val keyPatch = patch.asKey
        keyPatch.init
      }
      val key = bucket.findKey(keyName).get
      Commit.retry(() => key.versions.acquireNewLoc) { patch =>
        val version = patch.asVersion
        version.init

        Commit.retry(version.acl) { patch =>
          val dataPatch = patch.asData
          dataPatch.init

          dataPatch.writeBytes(Acl.t(callerId, Seq(
            Acl.Grant(
              Acl.ById(callerId),
              Acl.FullControl()
            )
          )).toBytes)
        }
        version.data.asData.writeBytes(objectData)
        version.meta.asData.writeBytes(
          Meta.t(
            isVersioned = false,
            isDeleteMarker = false,
            eTag = computedETag,
            attrs = HeaderList.builder
              .appendOpt("Content-Type", contentType)
              .appendOpt("Content-Disposition", contentDisposition)
              .build,
            xattrs = HeaderList.builder.build
          ).toBytes)
      }

      server.compactorQueue.queue(KeyCompactor(key))

      Ok()
        .withHeader(X_AMZ_REQUEST_ID -> requestId)
        .withHeader(X_AMZ_VERSION_ID -> "null")
        .withHeader(ETAG -> quoteString(computedETag))
        // TODO Origin
    }
  }
}
