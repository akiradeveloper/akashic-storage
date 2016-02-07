package akashic.storage.service

import akashic.storage.{HeaderList, server, files}
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.headers.ETag
import akka.http.scaladsl.model._
import com.google.common.net.HttpHeaders._

import akka.http.scaladsl.server.Directives._

object HeadObject {
  val matcher =
    head &
    GetObject.matcherCommon &
    provide("HEAD Object")

  // message bodies on responses to HEAD requests
  // TODO comment ported from akka-s3 but no evidence found
  val route =
    matcher.as(GetObject.t)(_.run)
}

object GetObject {
  val matcherCommon =
    extractObject &
    parameters(
      "versionId"?,
      "response-content-type"?,
      "response-content-language"?,
      "response-expires"?,
      "response-cache-control"?,
      "response-content-disposition"?,
      "response-content-encoding"?) &
    extractRequest

  val matcher =
    get &
    matcherCommon &
    provide("GET Object")

  val route = matcher.as(t)(_.run)
  case class t(
    bucketName: String, keyName: String,
    versionId: Option[String], // not used yet
    responseContentType: Option[String],
    responseContentLanguage: Option[String],
    responseExpires: Option[String],
    responseCacheControl: Option[String],
    responseContentDisposition: Option[String],
    responseContentEncoding: Option[String],
    req: HttpRequest,
    _name: String
  ) extends API {
    def name = _name
    def resource = Resource.forObject(bucketName, keyName)
    def runOnce = {
      val bucket = findBucket(server.tree, bucketName)
      val key = findKey(bucket, keyName)
      val version = key.findLatestVersion match {
        case Some(a) => a
        case None => failWith(Error.NoSuchKey())
      }
      // TODO if this is a delete marker?

      val meta = Meta.fromBytes(version.meta.read)
      
      val filePath = version.data.filePath

      val contentType = responseContentType <+ Some(files.detectContentType(filePath))

      val contentDisposition = responseContentDisposition <+ meta.attrs.find("Content-Disposition")

      val headers = ResponseHeaderList.builder
        .withHeader(X_AMZ_REQUEST_ID, requestId)
        .withHeader(`Last-Modified`(DateTime(files.lastDate(filePath).getTime)))
        .withHeader(ETag(meta.eTag))
        .withHeader(CONTENT_DISPOSITION, contentDisposition)
        .build

      val ct: ContentType = ContentType.parse(contentType.get).right.get
      complete(StatusCodes.OK, headers, HttpEntity(ct, filePath.toFile, 1 << 20))
    }
  }
}

