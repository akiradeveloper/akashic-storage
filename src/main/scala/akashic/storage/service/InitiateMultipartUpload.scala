package akashic.storage.service

import akashic.storage.{HeaderList, server}
import akashic.storage.patch.Commit
import akka.http.scaladsl.model.{StatusCodes, HttpRequest}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import scala.xml.NodeSeq
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._

object InitiateMultipartUpload {
  val matcher =
    post &
    extractObject &
    parameter("uploads").tflatMap(a => pass) & // FIXME not sure
    optionalHeaderValueByName("x-amz-acl") &
    extractGrantsFromHeaders &
    optionalHeaderValueByName("Content-Type") &
    optionalHeaderValueByName("Content-Disposition") &
    extractRequest
  val route = matcher.as(t)(_.run)
  case class t(bucketName: String, keyName: String,
               cannedAcl: Option[String],
               grantsFromHeaders: Iterable[Acl.Grant],
               contentType: Option[String],
               contentDisposition: Option[String],
               req: HttpRequest) extends AuthorizedAPI {
    def name = "Initiate Multipart Upload"
    def resource = Resource.forObject(bucketName, keyName)
    def runOnce = {
      val bucket = findBucket(server.tree, bucketName)
      val bucketAcl = Acl.fromBytes(bucket.acl.read)

      if (!bucketAcl.getPermission(callerId).contains(Acl.Write()))
        failWith(Error.AccessDenied())

      Commit.once(bucket.keyPath(keyName)) { patch =>
        val keyPatch = patch.asKey
        keyPatch.init
      }
      val key = bucket.findKey(keyName).get

      val uploadId = key.uploads.acquireNewUpload
      Commit.once(key.uploads.root.resolve(uploadId)) { patch =>
        val upload = patch.asUpload
        upload.init

        val grantsFromCanned = (cannedAcl <+ Some("private")).map(Acl.CannedAcl.forName(_, callerId, bucketAcl.owner)).map(_.makeGrants).get
        upload.acl.write(Acl.t(callerId, grantsFromCanned ++ grantsFromHeaders).toBytes)

        upload.meta.write(
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
