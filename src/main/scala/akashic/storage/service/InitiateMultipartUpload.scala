package akashic.storage.service

import akashic.storage.patch.{Commit, Key, Upload}
import akashic.storage.{HeaderList, server}
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._

object InitiateMultipartUpload {
  val matcher =
    post &
    extractObject &
    withParameter("uploads") &
    optionalHeaderValueByName("x-amz-acl") &
    extractGrantsFromHeaders &
    optionalHeaderValueByName("Content-Type") &
    optionalHeaderValueByName("Content-Disposition") &
    extractMetadata

  val route = matcher.as(t)(_.run)

  case class t(bucketName: String, keyName: String,
               cannedAcl: Option[String],
               grantsFromHeaders: Iterable[Acl.Grant],
               contentType: Option[String],
               contentDisposition: Option[String],
               metadata: HeaderList) extends AuthorizedAPI {
    def name = "Initiate Multipart Upload"
    def resource = Resource.forObject(bucketName, keyName)
    def runOnce = {
      val bucket = findBucket(server.tree, bucketName)
      val bucketAcl = bucket.acl.get

      if (!bucketAcl.grant(callerId, Acl.Write()))
        failWith(Error.AccessDenied())

      Commit.once(bucket.keyPath(keyName)) { patch =>
        val keyPatch = Key(bucket, patch.root)
        keyPatch.init
      }
      val key = bucket.findKey(keyName).get

      val uploadId = key.uploads.acquireNewUpload
      Commit.once(key.uploads.root.resolve(uploadId)) { patch =>
        val upload = Upload(patch.root)
        upload.init

        upload.acl.put {
          val grantsFromCanned = (cannedAcl <+ Some("private"))
            .map(Acl.CannedAcl.forName(_, callerId, bucketAcl.owner))
            .map(_.makeGrants).get
          Acl(callerId, grantsFromCanned ++ grantsFromHeaders)
        }

        upload.meta.put(
          Meta(
            versionId = "null",
            eTag = "",
            attrs = HeaderList.builder
              .appendOpt("Content-Type", contentType)
              .appendOpt("Content-Disposition", contentDisposition)
              .build,
            xattrs = metadata
          ))
      }
      val xml =
        <InitiateMultipartUploadResult>
          <Bucket>{bucketName}</Bucket>
          <Key>{decodeKeyName(keyName)}</Key>
          <UploadId>{uploadId}</UploadId>
        </InitiateMultipartUploadResult>

      val headers = ResponseHeaderList.builder
        .withHeader(X_AMZ_REQUEST_ID, requestId)
        .build

      complete(StatusCodes.OK, headers, xml)
    }
  }
}
