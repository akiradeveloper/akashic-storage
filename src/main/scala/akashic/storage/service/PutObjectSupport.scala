package akashic.storage.service

import akashic.storage.compactor.KeyCompactor
import akashic.storage.patch.Commit
import akashic.storage.{files, Server}
import akashic.storage.service.Error.Reportable
import io.finch._

trait PutObjectSupport {
  self: Server =>
  object PutObject {
    val matcher = put(
      string / string ?
      binaryBody ?
      headerOption("Content-Type") ?
      headerOption("Content-Disposition") ?
      RequestId.reader ?
      CallerId.reader).as[t]
    val endpoint = matcher { a: t => a.run }
    case class t(bucketName: String, keyName: String,
                 objectData: Array[Byte],
                 contentType: Option[String],
                 contentDisposition: Option[String],
                 requestId: String,
                 callerId: String) extends Task[Output[Unit]] with Reportable {
      def name = "PUT Object"
      def resource = Resource.forObject(bucketName, keyName)
      def runOnce = {
        val computedETag = files.computeMD5(objectData)
        val bucket = findBucket(tree, bucketName)
        Commit.once(bucket.keyPath(keyName)) { patch => 
          val keyPatch = patch.asKey
          keyPatch.init
        }
        val key = bucket.findKey(keyName).get
        Commit.retry(key.versions) { patch =>
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
              attrs = KVList.builder
                .appendOpt("Content-Type", contentType)
                .appendOpt("Content-Disposition", contentDisposition)
                .build,
              xattrs = KVList.builder.build
            ).toBytes)
        }

        compactorQueue.queue(KeyCompactor(key, self))

        Ok()
          .withHeader(X_AMZ_REQUEST_ID -> requestId)
          .withHeader(X_AMZ_VERSION_ID -> "null")
          .withHeader("ETag" -> computedETag)
          // TODO Origin
      }
    }
  }
}
