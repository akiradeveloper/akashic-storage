package akashic.storage.service

import java.nio.file.Path

import akashic.storage.{HeaderList, server}
import akashic.storage.patch.Commit
import akashic.storage.patch.Commit.RetryGenericNoCommit
import akashic.storage.service.Error.Reportable
import com.twitter.finagle.http.Request
import io.finch._

import scala.xml.NodeSeq

object InitiateMultipartUpload {
  val matcher = post(keyMatcher / paramExists("uploads") ?
    headerOption("Content-Type") ?
    headerOption("Content-Disposition") ?
    extractRequest).as[t]
  val endpoint = matcher { a: t => a.run }
  case class t(bucketName: String, keyName: String,
               contentType: Option[String],
               contentDisposition: Option[String],
               req: Request) extends Task[Output[NodeSeq]] {
    def name = "Initiate Multipart Upload"
    def resource = Resource.forObject(bucketName, keyName)
    def runOnce = {
      val bucket = findBucket(server.tree, bucketName)
      Commit.once(bucket.keyPath(keyName)) { patch =>
        val keyPatch = patch.asKey
        keyPatch.init
      }
      val key = bucket.findKey(keyName).get
      val reservedPatch = RetryGenericNoCommit(() => key.versions.acquireNewLoc) { patch =>
        // no init. just allocate the dir
      }.run
      val uploadId = key.uploads.acquireNewUpload(reservedPatch.name)
      Commit.once(key.uploads.root.resolve(uploadId)) { patch =>
        val upload = patch.asUpload
        upload.init

        upload.acl.writeBytes(Acl.t(callerId, Seq(
            Acl.Grant(
              Acl.ById(callerId),
              Acl.FullControl()
            )
          )).toBytes)

        upload.meta.asData.writeBytes(
          Meta.t(
            isVersioned = false,
            isDeleteMarker = false,
            eTag = "",
            attrs = HeaderList.builder
              .appendOpt("Content-Type", contentType)
              .appendOpt("Content-Disposition", contentDisposition)
              .build,
            xattrs = HeaderList.builder.build
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
