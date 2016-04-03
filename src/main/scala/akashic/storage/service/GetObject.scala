package akashic.storage.service

import akashic.storage.backend.NodePath
import akashic.storage.server
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{ETag, _}
import akka.http.scaladsl.server.Directives._
import com.google.common.net.HttpHeaders._

object HeadObject {
  val matcher =
    head &
    GetObject.matcherCommon &
    provide("HEAD Object")

  // (from description on transparant-head-requests)
  // Note that, even when this setting is off the server will never send
  // out message bodies on responses to HEAD requests.
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
      "response-content-encoding"?)

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
    _name: String
  ) extends AuthorizedAPI {
    def name = _name
    def resource = Resource.forObject(bucketName, keyName)
    def runOnce = {
      val bucket = findBucket(server.tree, bucketName)
      val key = findKey(bucket, keyName)

      val version = key.findLatestVersion match {
        case Some(a) => a
        case None => failWith(Error.NoSuchKey())
      }

      val versionAcl = version.acl.get
      if (!versionAcl.grant(callerId, Acl.Read()))
        failWith(Error.AccessDenied())

      // TODO if this is a delete marker?

      val meta = version.meta.get
      
      val filePath: NodePath = version.data.filePath

      val contentType = responseContentType <+ Some(filePath.detectContentType)

      val contentDisposition = responseContentDisposition <+ meta.attrs.find("Content-Disposition")

      val lastModified = DateTime(filePath.getAttr.creationTime)

      val headers = ResponseHeaderList.builder
        .withHeader(X_AMZ_REQUEST_ID, requestId)
        .withHeader(`Last-Modified`(lastModified))
        .withHeader(ETag(meta.eTag))
        .withHeader(CONTENT_DISPOSITION, contentDisposition)
        .withHeader(meta.xattrs.unwrap)
        .build

      val ct: ContentType = ContentType.parse(contentType.get).right.get
      conditional(EntityTag(meta.eTag), lastModified) {
        withRangeSupport {
          complete(StatusCodes.OK, headers, HttpEntity.Default(ct, filePath.getAttr.length, filePath.getSource(1 << 20)))
        }
      }
    }
  }
}

