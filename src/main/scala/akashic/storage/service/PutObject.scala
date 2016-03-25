package akashic.storage.service

import akashic.storage.patch.{Version, Key, Commit}
import akashic.storage.{HeaderList, server}
import akka.http.scaladsl.model.headers.ETag
import akka.http.scaladsl.model.{HttpEntity, StatusCodes, HttpRequest}
import akka.http.scaladsl.server.Route
import com.google.common.net.HttpHeaders._
import akka.http.scaladsl.server.Directives._
import org.apache.commons.codec.binary.{Hex, Base64}
import org.apache.commons.codec.digest.DigestUtils
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
               contentMd5: Option[String],
               metadata: HeaderList.t,
               callerId: String,
               requestId: String) extends Task[Result] with Error.Reportable {
    override def resource: String = Resource.forObject(bucketName, keyName)
    override def runOnce: Result = {
      val computedMD5 = DigestUtils.md5(objectData)
      for (md5 <- contentMd5)
        if (Base64.encodeBase64String(computedMD5) != md5)
          failWith(Error.BadDigest())

      val computedETag = Hex.encodeHexString(computedMD5)

      val bucket = findBucket(server.tree, bucketName)
      val bucketAcl = bucket.acl.get

      if (!bucketAcl.grant(callerId, Acl.Write()))
        failWith(Error.AccessDenied())

      Commit.once(bucket.keyPath(keyName)) { patch =>
        val keyPatch = Key(bucket, patch.root)
        keyPatch.init
      }
      val key = bucket.findKey(keyName).get
      Commit.replaceDirectory(key.versions.acquireWriteDest) { patch =>
        val version = Version(key, patch.root)

        version.acl.put {
          val grantsFromCanned = (cannedAcl <+ Some("private"))
            .map(Acl.CannedAcl.forName(_, callerId, bucketAcl.owner))
            .map(_.makeGrants).get
          Acl.t(callerId, grantsFromCanned ++ grantsFromHeaders)
        }
        version.data.put(objectData)
        version.meta.put(
          Meta.t(
            isVersioned = false,
            isDeleteMarker = false,
            eTag = computedETag,
            attrs = HeaderList.builder
              .appendOpt("Content-Type", contentType)
              .appendOpt("Content-Disposition", contentDisposition)
              .build,
            xattrs = metadata
          ))
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
    optionalHeaderValueByName("Content-MD5") &
    extractMetadata &
    extractRequest
  val route = matcher.as(t)(_.run)
  case class t(bucketName: String, keyName: String,
               objectData: Array[Byte],
               cannedAcl: Option[String],
               grantsFromHeaders: Iterable[Acl.Grant],
               contentType: Option[String],
               contentDisposition: Option[String],
               contentMd5: Option[String],
               metadata: HeaderList.t,
               req: HttpRequest) extends AuthorizedAPI {
    def name = "PUT Object"
    def resource = Resource.forObject(bucketName, keyName)
    def runOnce = {
      val result = MakeObject.t(bucketName, keyName, objectData, cannedAcl, grantsFromHeaders, contentType, contentDisposition, contentMd5, metadata, callerId, requestId).run
      val headers = ResponseHeaderList.builder
        .withHeader(X_AMZ_REQUEST_ID, requestId)
        .withHeader(X_AMZ_VERSION_ID, result.versionId)
        .withHeader(ETag(result.etag))
        .build
      complete(StatusCodes.OK, headers, HttpEntity.Empty)
    }
  }
}
