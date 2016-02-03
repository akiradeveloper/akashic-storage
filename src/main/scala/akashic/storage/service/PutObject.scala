package akashic.storage.service

import akashic.storage.patch.Commit
import akashic.storage.{HeaderList, files, server}
import com.twitter.concurrent.AsyncStream
import com.twitter.finagle.http.Request
import com.google.common.net.HttpHeaders._
import com.twitter.io.Buf
import io.finch._

object PutObject {
  val matcher = put(
    keyMatcher ?
    asyncBody ?
    headerOption("Content-Type") ?
    headerOption("Content-Disposition") ?
    extractRequest)
  val endpoint = matcher {
    (bucketName: String, keyName: String,
     objectData: AsyncStream[Buf],
     contentType: Option[String],
     contentDisposition: Option[String],
     req: Request) =>
     for {
      od <- mkByteArray(objectData)
     } yield t(bucketName, keyName, od, contentType, contentDisposition, req).run
  }

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
      Commit.replaceDirectory(key.versions.acquireWriteDest) { patch =>
        val version = patch.asVersion

        Commit.replaceData(version.acl) { patch =>
          val dataPatch = patch.asData
          dataPatch.write(Acl.t(callerId, Seq(
            Acl.Grant(
              Acl.ById(callerId),
              Acl.FullControl()
            )
          )).toBytes)
        }
        version.data.write(objectData)
        version.meta.write(
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

      Ok()
        .withHeader(X_AMZ_REQUEST_ID -> requestId)
        .withHeader(X_AMZ_VERSION_ID -> "null")
        .withHeader(ETAG -> quoteString(computedETag))
        // TODO Origin
    }
  }
}
