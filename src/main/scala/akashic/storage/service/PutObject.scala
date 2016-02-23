package akashic.storage.service

import akashic.storage.patch.Commit
import akashic.storage.{HeaderList, files, server}
import akka.http.scaladsl.model.headers.ETag
import akka.http.scaladsl.model.{HttpEntity, StatusCodes, HttpRequest}
import akka.http.scaladsl.server.Route
import com.google.common.net.HttpHeaders._
import akka.http.scaladsl.server.Directives._
import scala.collection.immutable

object MakeObject {
  case class Result(versionId: String, etag: String)
  // TODO use acl
  case class t(bucketName: String, keyName: String,
               objectData: Array[Byte],
               cannedAcl: Option[String],
               grantsFromHeaders: Iterable[Acl.Grant],
               contentType: Option[String],
               contentDisposition: Option[String],
               metadata: HeaderList.t,
               callerId: String,
               requestId: String) extends Task[Result] with Error.Reportable {
    override def resource: String = Resource.forObject(bucketName, keyName)
    override def runOnce: Result = {
      val computedETag = files.computeMD5(objectData)
      val bucket = findBucket(server.tree, bucketName)
      val bucketAcl = Acl.fromBytes(bucket.acl.read)

      if (!bucketAcl.getPermission(callerId).contains(Acl.Write()))
        failWith(Error.AccessDenied())

      Commit.once(bucket.keyPath(keyName)) { patch =>
        val keyPatch = patch.asKey
        keyPatch.init
      }
      val key = bucket.findKey(keyName).get
      Commit.replaceDirectory(key.versions.acquireWriteDest) { patch =>
        val version = patch.asVersion

        Commit.replaceData(version.acl) { data =>
          val grantsFromCanned = (cannedAcl <+ Some("private")).map(Acl.CannedAcl.forName(_, callerId, bucketAcl.owner)).map(_.makeGrants).get
          data.write(Acl.t(callerId, grantsFromCanned ++ grantsFromHeaders).toBytes)
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
            xattrs = metadata
          ).toBytes)
      }
      Result("null", computedETag)
    }
  }
}

object PutObject {
  val matcher = put &
    extractObject &
    entity(as[Array[Byte]]) &
    optionalHeaderValueByName("x-amz-acl") &
    extractGrantsFromHeaders &
    optionalHeaderValueByName("Content-Type") &
    optionalHeaderValueByName("Content-Disposition") &
    extractMetadata &
    extractRequest
  val route = matcher.as(t)(_.run)
  case class t(bucketName: String, keyName: String,
               objectData: Array[Byte],
               cannedAcl: Option[String],
               grantsFromHeaders: Iterable[Acl.Grant],
               contentType: Option[String],
               contentDisposition: Option[String],
               metadata: HeaderList.t,
               req: HttpRequest) extends AuthorizedAPI {
    def name = "PUT Object"
    def resource = Resource.forObject(bucketName, keyName)
    def runOnce = {
      val result = MakeObject.t(bucketName, keyName, objectData, cannedAcl, grantsFromHeaders, contentType, contentDisposition, metadata, callerId, requestId).run
      val headers = ResponseHeaderList.builder
        .withHeader(X_AMZ_REQUEST_ID, requestId)
        .withHeader(X_AMZ_VERSION_ID, result.versionId)
        .withHeader(ETag(result.etag))
        .build
      complete(StatusCodes.OK, headers, HttpEntity.Empty)
    }
  }
}
