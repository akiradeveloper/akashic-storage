package akashic.storage.service

import akashic.storage.server
import akashic.storage.patch.{Data, Commit}
import akka.http.scaladsl.model.{StatusCodes, HttpEntity, HttpRequest}
import akka.http.scaladsl.model.headers.ETag
import akka.http.scaladsl.server.Route
import com.google.common.net.HttpHeaders._
import akka.http.scaladsl.server.Directives._
import org.apache.commons.codec.binary.{Hex, Base64}
import org.apache.commons.codec.digest.DigestUtils

object UploadPart {
  val matcher =
    put &
    extractObject &
    parameters("uploadId", "partNumber".as[Int]) &
    entity(as[Array[Byte]]) &
    optionalHeaderValueByName("Content-MD5") &
    extractRequest

  val route = matcher.as(t)(_.run)

  case class t(bucketName: String, keyName: String,
               uploadId: String,
               partNumber: Int,
               partData: Array[Byte],
               contentMd5: Option[String],
               req: HttpRequest) extends AuthorizedAPI {
    def name = "Upload Part"
    def resource = Resource.forObject(bucketName, keyName)
    def runOnce = {
      val bucket = findBucket(server.tree, bucketName)
      val key = findKey(bucket, keyName, Error.NoSuchUpload())
      val upload = findUpload(key, uploadId)

      val computedMD5 = DigestUtils.md5(partData)
      for (md5 <- contentMd5)
        if (Base64.encodeBase64String(computedMD5) != md5)
          failWith(Error.BadDigest())

      val computedETag = Hex.encodeHexString(computedMD5)

      val part = upload.part(partNumber)
      Commit.replaceData(part.unwrap, Data.Pure.make) { data =>
        data.put(partData)
      }

      val headers = ResponseHeaderList.builder
        .withHeader(X_AMZ_REQUEST_ID, requestId)
        .withHeader(ETag(computedETag))
        .build

      complete(StatusCodes.OK, headers, HttpEntity.Empty)
    }
  }
}
