package akashic.storage.service

import com.twitter.concurrent.AsyncStream
import com.twitter.finagle.http.Request
import io.finch._
import akashic.storage.{HeaderList, server, files}
import com.twitter.io.{Reader, Buf}
import com.google.common.net.HttpHeaders._

object HeadObject {
 val matcher = head(keyMatcher ?
    paramOption("versionId") ?
    paramOption("response-content-type") ?
    paramOption("response-content-language") ?
    paramOption("response-expires") ?
    paramOption("response-cache-control") ?
    paramOption("response-content-disposition") ?
    paramOption("response-content-encoding") ?
    extractRequest ?
    RequestReader.value(false) ?
    RequestReader.value("Head Object")
    ).as[GetObject.t]
 val endpoint = matcher { a: GetObject.t => a.run }
}
object GetObject {
  val matcher = get(keyMatcher ?
    paramOption("versionId") ?
    paramOption("response-content-type") ?
    paramOption("response-content-language") ?
    paramOption("response-expires") ?
    paramOption("response-cache-control") ?
    paramOption("response-content-disposition") ?
    paramOption("response-content-encoding") ?
    extractRequest ?
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
    req: Request,
    withContent: Boolean,
    label: String
  ) extends Task[Output[AsyncStream[Buf]]] {
    def name = label
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

      val buf: AsyncStream[Buf] = if (withContent) {
        val reader = Reader.fromFile(filePath.toFile)
        AsyncStream.fromFuture(Reader.readAll(reader))
      } else {
        AsyncStream.empty
      }

      // TODO use this
      val headers = HeaderList.builder
        .appendOpt(CONTENT_DISPOSITION, contentDisposition)
        // TODO (others)
        .build

      println(dates.formatLastModified(files.lastDate(filePath)))
      Ok(buf).append(headers)
        .withContentType(contentType)
        .withHeader(X_AMZ_REQUEST_ID -> requestId)
        .withHeader(ETAG -> quoteString(meta.eTag))
        .withHeader(CONTENT_LENGTH -> files.fileSize(filePath).toString)
        .withHeader(LAST_MODIFIED -> dates.formatLastModified(files.lastDate(filePath)))
    }
  }
}

