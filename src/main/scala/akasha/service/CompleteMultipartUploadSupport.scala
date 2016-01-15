package akasha.service

import akasha.Server
import akasha.service.Error.Reportable
import com.twitter.util.Future

import scala.util.{Failure, Success, Try}
import scala.xml.{XML, NodeSeq}
import io.finch._

trait CompleteMultipartUploadSupport {
  self: Server =>
  object CompleteMultipartUpload {
    val matcher = post(string / string ?
      param("uploadId") ?
      body ?
      RequestId.reader ?
      CallerId.reader
    ).as[t]
    val endpoint = matcher { a: t => a.run }
    case class t(bucketName: String, keyName: String,
                 uploadId: String,
                 data: String,
                 requestId: String, callerId: String) extends Task[Output[Future[NodeSeq]]] with Reportable {
      def resource = Resource.forObject(bucketName, keyName)
      def runOnce = {
        case class Part(partNumber: Int, eTag: String)
        val bucket = findBucket(tree, bucketName)
        val key = findKey(bucket, keyName)
        val upload = findUpload(key, uploadId)

        val parts = Try {
          val xml = XML.loadString(data)
          (xml \ "Part").map { a =>
            Part(
              (a \ "PartNumber").text.toInt,
              (a \ "ETag").text.replace("\"", "")
            )
          }
        } match {
          case Success(a) => a
          case Failure(_) => failWith(Error.MalformedXML())
        }

        // parts must be sorted
        if (parts != parts.sortBy(_.partNumber))
          failWith(Error.InvalidPartOrder())

        Ok(Future(NodeSeq.Empty))
      }
    }
  }
}
