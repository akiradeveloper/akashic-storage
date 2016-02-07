package akashic.storage.service

import akashic.storage.{files, server}
import akashic.storage.patch.Commit
import akka.http.scaladsl.model.{StatusCodes, HttpEntity, HttpRequest}
import akka.http.scaladsl.model.headers.ETag
import akka.http.scaladsl.server.Route
import com.google.common.net.HttpHeaders._
import akka.http.scaladsl.server.Directives._

object UploadPart {
  val matcher =
    put &
    extractObject &
    parameters("uploadId", "partNumber".as[Int]) &
    entity(as[Array[Byte]]) &
    extractRequest

  val route = matcher.as(t)(_.run)

  case class t(bucketName: String, keyName: String,
               uploadId: String,
               partNumber: Int,
               partData: Array[Byte],
               req: HttpRequest) extends Task[Route] {
    def name = "Upload Part"
    def resource = Resource.forObject(bucketName, keyName)
    def runOnce = {
      val bucket = findBucket(server.tree, bucketName)
      val key = findKey(bucket, keyName)
      val upload = findUpload(key, uploadId)

      val computedMD5 = files.computeMD5(partData)
      val part = upload.part(partNumber)
      Commit.replaceData(part.unwrap) { patch =>
        val dataPatch = patch.asData
        dataPatch.write(partData)
      }

      val headers = ResponseHeaderList.builder
        .withHeader(X_AMZ_REQUEST_ID, requestId)
        .withHeader(ETag(computedMD5))
        .build

      complete(StatusCodes.OK, headers, HttpEntity.Empty)
    }
  }
}
