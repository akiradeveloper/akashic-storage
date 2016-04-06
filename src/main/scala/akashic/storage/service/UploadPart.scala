package akashic.storage.service

import java.io.InputStream

import akashic.storage.patch.{Commit, Data}
import akashic.storage.server
import akka.http.scaladsl.model.headers.ETag
import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import org.apache.commons.codec.binary.{Base64, Hex}
import org.apache.commons.codec.digest.DigestUtils

object UploadPart {
  val matcher =
    put &
    extractObject &
    parameters("uploadId", "partNumber".as[Int]) &
    entityAsInputStream &
    optionalHeaderValueByName("Content-MD5")

  val route = matcher.as(t)(_.run)

  case class t(bucketName: String, keyName: String,
               uploadId: String,
               partNumber: Int,
               partData: InputStream,
               contentMd5: Option[String]) extends AuthorizedAPI {
    def name = "Upload Part"
    def resource = Resource.forObject(bucketName, keyName)
    def runOnce = {
      val bucket = findBucket(server.tree, bucketName)
      val key = findKey(bucket, keyName, Error.NoSuchUpload())
      val upload = findUpload(key, uploadId)
      val part = upload.part(partNumber)
      var computedETag = ""
      Commit.replaceData(part.unwrap, Data.Pure.make) { data =>
        data.filePath.createFile(partData)

        val computedMD5 = DigestUtils.md5(data.filePath.getInputStream)
        for (md5 <- contentMd5)
          if (Base64.encodeBase64String(computedMD5) != md5)
            failWith(Error.BadDigest())
        computedETag = Hex.encodeHexString(computedMD5)
      }

      val headers = ResponseHeaderList.builder
        .withHeader(X_AMZ_REQUEST_ID, requestId)
        .withHeader(ETag(computedETag))
        .build

      complete(StatusCodes.OK, headers, HttpEntity.Empty)
    }
  }
}
