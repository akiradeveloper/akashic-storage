package akashic.storage.service

import akashic.storage.patch.{Key, Bucket}
import akka.http.scaladsl.model.{HttpEntity, StatusCodes, HttpRequest}
import akashic.storage.server
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

object DeleteObject {
  def deleteObject(bucket: Bucket, key: Key, versionId: Option[Int]): Option[Int] = {
    // x-amz-delete-marker
    // Specifies whether the versioned object that was permanently deleted was (true) or was not (false) a delete marker.
    // In a simple DELETE, this header indicates whether (true) or not (false) a delete marker was created.
    //
    // x-amz-version-id
    // Returns the version ID of the delete marker created as a result of the DELETE operation.
    // If you delete a specific object version, the value returned by this header is the version ID of the object version deleted.
    val versioning = Versioning.fromBytes(bucket.versioning.read).value
    if (versionId.isDefined) {
      assert(false)
      None
    } else {
      // simple DELETE
      versioning match {
        case Versioning.UNVERSIONED =>
          server.astral.free(key.versions.root.resolve("0"))
          None
        case Versioning.ENABLED =>
          assert(false)
          None
      }
    }
  }

  val matcher =
    delete &
    extractObject &
    parameters("versionId".as[Int]?) &
    extractRequest

  val route = matcher.as(t)(_.run)
  case class t(bucketName: String, keyName: String,
               versionId: Option[Int],
               req: HttpRequest) extends AuthorizedAPI {
    def name = "DELETE Object"
    def resource = Resource.forObject(bucketName, keyName)
    def runOnce = {
      val bucket = findBucket(server.tree, bucketName)
      val key = findKey(bucket, keyName)

      val deleteResult = deleteObject(bucket, key, versionId)
      assert(deleteResult == None)

      val headers = ResponseHeaderList.builder
        .withHeader(X_AMZ_REQUEST_ID, requestId)
        .withHeader(X_AMZ_DELETE_MARKER, "false")
        .withHeader(X_AMZ_VERSION_ID, "null")
        .build

      complete(StatusCodes.NoContent, headers, HttpEntity.Empty)
    }
  }
}


