package akashic.storage.service

import akashic.storage.patch.Commit
import akashic.storage.{HeaderList, files, server}
import akka.http.scaladsl.model.headers.ETag
import akka.http.scaladsl.model.{HttpEntity, StatusCodes, HttpRequest}
import akka.http.scaladsl.server.Route
import com.google.common.net.HttpHeaders._
import akka.http.scaladsl.server.Directives._
import scala.collection.immutable

object PutObject {
  val matcher = put &
    extractObject &
    entity(as[Array[Byte]]) &
    optionalHeaderValueByName("Content-Type") &
    optionalHeaderValueByName("Content-Disposition") &
    extractRequest

  val route = matcher.as(t)(_.run)

  case class t(bucketName: String, keyName: String,
               objectData: Array[Byte],
               contentType: Option[String],
               contentDisposition: Option[String],
               req: HttpRequest) extends AuthorizedAPI {
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
            xattrs = HeaderList.builder
              .build
          ).toBytes)
      }

      val headers = ResponseHeaderList.builder
        .withHeader(X_AMZ_REQUEST_ID, requestId)
        .withHeader(X_AMZ_VERSION_ID, "null")
        .withHeader(ETag(computedETag))
        .build

      complete(StatusCodes.OK, headers, HttpEntity.Empty)
    }
  }
}
