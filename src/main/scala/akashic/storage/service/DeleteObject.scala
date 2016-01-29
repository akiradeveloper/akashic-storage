package akashic.storage.service

import akashic.storage.patch.Commit
import com.twitter.finagle.http.Request
import io.finch._
import akashic.storage.{HeaderList, server}

object DeleteObject {
  val matcher = delete(keyMatcher ?
    paramOption("versionId").as[Int] ?
    extractRequest
  ).as[t]
  val endpoint = matcher { a: t => a.run }
  case class t(bucketName: String, keyName: String,
               versionId: Option[Int],
               req: Request) extends Task[Output[Unit]] {
    def name = "DELETE Object"
    def resource = Resource.forObject(bucketName, keyName)
    def runOnce = {
      val bucket = findBucket(server.tree, bucketName)
      val key = findKey(bucket, keyName)
      val versioning = Versioning.fromBytes(bucket.versioning.readBytes).value

      // x-amz-delete-marker
      // Specifies whether the versioned object that was permanently deleted was (true) or was not (false) a delete marker.
      // In a simple DELETE, this header indicates whether (true) or not (false) a delete marker was created.
      //
      // x-amz-version-id
      // Returns the version ID of the delete marker created as a result of the DELETE operation.
      // If you delete a specific object version, the value returned by this header is the version ID of the object version deleted.
      if (versionId.isDefined) {
        NoContent[Unit]
      } else {
        // simple DELETE
        
        versioning match {
          case Versioning.UNVERSIONED =>
            server.astral.free(key.versions.root.resolve("0"))
          case Versioning.ENABLED =>
            assert(false)
        }

        // val patch = Commit.retry(() => key.versions.acquireNewLoc) { patch =>
        //   val version = patch.asVersion
        //
        //   // default acl
        //   Commit.replace(version.acl) { patch =>
        //     val dataPatch = patch.asData
        //     dataPatch.writeBytes(Acl.t(callerId, Seq(
        //       Acl.Grant(
        //         Acl.ById(callerId),
        //         Acl.FullControl()
        //       )
        //     )).toBytes)
        //   }
        //   version.meta.writeBytes(
        //     Meta.t(
        //       isVersioned = false,
        //       isDeleteMarker = true,
        //       eTag = "",
        //       attrs = HeaderList.builder.build,
        //       xattrs = HeaderList.builder.build
        //     ).toBytes
        //   )
        // }

        NoContent[Unit]
          .withHeader(X_AMZ_REQUEST_ID -> requestId)
          .withHeader(X_AMZ_DELETE_MARKER -> "false")
          .withHeader(X_AMZ_VERSION_ID -> "null")
      }
    }
  }
}


