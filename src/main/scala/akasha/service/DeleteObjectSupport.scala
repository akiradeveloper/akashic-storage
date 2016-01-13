package akasha.service

import akasha.patch.Commit
import akasha.service.Error.Reportable
import io.finch._
import akasha.Server

trait DeleteObjectSupport {
  self: Server =>
  object DeleteObject {
    val matcher = delete(string / string ?
      paramOption("versionId").as[Int] ?
      RequestId.reader ?
      CallerId.reader
    ).as[t]
    val endpoint = matcher { a: t => a.run }
    case class t(bucketName: String, keyName: String,
                 versionId: Option[Int],
                 requestId: String,
                 callerId: String) extends Task[Output[Unit]] with Reportable {
      def resource = Resource.forObject(bucketName, keyName)
      def runOnce = {
        val bucket = tree.findBucket(bucketName) match {
          case Some(a) => a
          case None => failWith(Error.NoSuchBucket())
        }
        val key = bucket.findKey(keyName) match {
          case Some(a) => a
          case None => failWith(Error.NoSuchKey())
        }
        val versioningEnabled = Versioning.fromBytes(bucket.versioning.get.get.asData.readBytes).enabled

        // x-amz-delete-marker
        // Specifies whether the versioned object that was permanently deleted was (true) or was not (false) a delete marker.
        // In a simple DELETE, this header indicates whether (true) or not (false) a delete marker was created.
        //
        // x-amz-version-id
        // Returns the version ID of the delete marker created as a result of the DELETE operation.
        // If you delete a specific object version, the value returned by this header is the version ID of the object version deleted.
        if (versionId.isDefined) {
          Ok()
        } else {
          // simple DELETE
          val patch = Commit.retry(key.versions) { patch =>
            val version = patch.asVersion
            version.init
            // default acl
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
            version.meta.asData.writeBytes(
              Meta.t(
                isVersioned = false,
                isDeleteMarker = true,
                eTag = "",
                attrs = KVList.builder.build,
                xattrs = KVList.builder.build
              ).toBytes
            )
          }
          Ok()
            .withHeader("x-amz-request-id" -> requestId)
            .withHeader("x-amz-delete-marker" -> "true")
            .withHeader("x-amz-version-id" -> (if (versioningEnabled) { patch.name } else { "null" }))
        }
      }
    }
  }
}

