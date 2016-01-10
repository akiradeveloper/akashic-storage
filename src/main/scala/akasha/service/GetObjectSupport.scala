package akasha.service
import io.finch._
import akasha.Server
import akasha.service.Error.Reportable
import com.twitter.io.Buf
import akasha.files

trait GetObjectSupport {
  self: Server =>
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
      CallerId.reader
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
      callerid: String
    ) extends Task[Output[Buf]] with Reportable {
      def resource = bucketName + "/" + keyName
      def runOnce = {
        val bucket = tree.findBucket(bucketName) match {
          case Some(a) => a
          case None => failWith(Error.NoSuchBucket())
        }
        val key = bucket.findKey(keyName) match {
          case Some(a) => a
          case None => failWith(Error.NoSuchKey())
        }
        val version = key.findLatestVersion match {
          case Some(a) => a
          case None => failWith(Error.NoSuchKey())
        }
        // TODO if this is a delete marker?
        
        val filePath = version.data.data
        val computedContentType = None
        val objectData: Array[Byte] = Array()
        val contentType = responseContentType <+ Some(files.detectContentType(filePath))
        val buf = Buf.Empty; buf.write(objectData, 0)
        Ok(buf)
      }
    }
  }
}
