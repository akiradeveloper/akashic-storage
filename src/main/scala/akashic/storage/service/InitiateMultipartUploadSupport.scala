package akashic.storage.service

import java.nio.file.Path

import akashic.storage.Server
import akashic.storage.patch.Commit
import akashic.storage.patch.Commit.RetryGenericNoCommit
import akashic.storage.service.Error.Reportable
import io.finch._

import scala.xml.NodeSeq

trait InitiateMultipartUploadSupport {
  self: Server =>
  object InitiateMultipartUpload {
    val matcher = post(string / string ? param("uploads") ?
      headerOption("Content-Type") ?
      headerOption("Content-Disposition") ?
      RequestId.reader ?
      CallerId.reader).as[t]
    val endpoint = matcher { a: t => a.run }
    case class t(bucketName: String, keyName: String,
                 uploads: String,
                 contentType: Option[String],
                 contentDisposition: Option[String],
                 requestId: String,
                 callerId: String) extends Task[Output[NodeSeq]] with Reportable {
      def resource = Resource.forObject(bucketName, keyName)
      def runOnce = {
        val bucket = findBucket(tree, bucketName)
        Commit.once(bucket.keyPath(keyName)) { patch =>
          patch.asKey.init
        }
        val key = bucket.findKey(keyName).get
        val reservedPatch = RetryGenericNoCommit(() => key.versions.acquireNewLoc) { patch =>
          // no init. just allocate the dir
        }.run
        val uploadId = key.uploads.acquireNewUpload(reservedPatch.name)
        Commit.once(key.uploads.root.resolve(uploadId)) { patch =>
          val upload = patch.asUpload
          upload.init
          Commit.retry(upload.acl) { patch =>
            val dataPatch = patch.asData
            dataPatch.init
            dataPatch.writeBytes(Acl.t(callerId, Seq(
              Acl.Grant(
                Acl.ById(callerId),
                Acl.FullControl()
              )
            )).toBytes)
          }
          upload.meta.asData.writeBytes(
            Meta.t(
              isVersioned = false,
              isDeleteMarker = false,
              eTag = "",
              attrs = KVList.builder
                .appendOpt("Content-Type", contentType)
                .appendOpt("Content-Disposition", contentDisposition)
                .build,
              xattrs = KVList.builder.build
            ).toBytes)
        }
        val xml =
          <InitiateMultipartUploadResult>
            <Bucket>{bucketName}</Bucket>
            <Key>{keyName}</Key>
            <UploadId>{uploadId}</UploadId>
          </InitiateMultipartUploadResult>
        Ok(xml)
          .withHeader(X_AMZ_REQUEST_ID -> requestId)
      }
    }
  }
}
