package akashic.storage.service

import io.finch._
import akashic.storage.Server
import akashic.storage.service.Error.Reportable
import akashic.storage.files
import com.twitter.io.Buf
import com.google.common.net.HttpHeaders._

trait GetObjectSupport {
  self: Server =>
  object HeadObject {
   val matcher = head(string / string ?
      paramOption("versionId") ?
      paramOption("response-content-type") ?
      paramOption("response-content-language") ?
      paramOption("response-expires") ?
      paramOption("response-cache-control") ?
      paramOption("response-content-disposition") ?
      paramOption("response-content-encoding") ?
      RequestId.reader ?
      CallerId.reader ?
      RequestReader.value(false) ?
      RequestReader.value("Head Object")
      ).as[GetObject.t]
   val endpoint = matcher { a: GetObject.t => a.run }
  }
  object GetObject {
    val matcher = get(string / string ?
      paramOption("versionId") ?
      paramOption("response-content-type") ?
      paramOption("response-content-language") ?
      paramOption("response-expires") ?
      paramOption("response-cache-control") ?
      paramOption("response-content-disposition") ?
      paramOption("response-content-encoding") ?
      RequestId.reader ?
      CallerId.reader ?
      RequestReader.value(true) ?
      RequestReader.value("Get Object")
      ).as[t]
    val endpoint = matcher { a: t => a.run }

    case class t(
      bucketName: String, keyName: String,
      versionId: Option[String], // not used yet
      responseContentType: Option[String],
      responseContentLanguage: Option[String],
      responseExpires: Option[String],
      responseCacheControl: Option[String],
      responseContentDisposition: Option[String],
      responseContentEncoding: Option[String],
      requestId: String,
      callerid: String,
      withContent: Boolean,
      label: String
    ) extends Task[Output[Buf]] with Reportable {
      def name = label
      def resource = Resource.forObject(bucketName, keyName)
      def runOnce = {
        val bucket = findBucket(tree, bucketName)
        val key = findKey(bucket, keyName)
        val version = key.findLatestVersion match {
          case Some(a) => a
          case None => failWith(Error.NoSuchKey())
        }
        // TODO if this is a delete marker?

        val meta = Meta.fromBytes(version.meta.readBytes)
        
        val filePath = version.data.filePath
        val contentType = responseContentType <+ Some(files.detectContentType(filePath))
        val contentDisposition = responseContentDisposition <+ meta.attrs.find("Content-Disposition")

        val buf = if (withContent) {
          val objectData = version.data.readBytes
          Buf.ByteArray.Owned(objectData)
        } else {
          Buf.Empty
        }

        // TODO use this
        val headers = KVList.builder
          .appendOpt(CONTENT_DISPOSITION, contentDisposition)
          // TODO (others)
          .build

        Ok(buf).append(headers)
          .withContentType(contentType)
          .withHeader(X_AMZ_REQUEST_ID -> requestId)
          .withHeader(ETAG -> meta.eTag)
          .withHeader(CONTENT_LENGTH -> buf.length.toString)
          // TODO Last-Modified
      }
    }
  }
}
