package akasha.service

import akasha.service.Error.Reportable
import com.twitter.util.Future

import scala.xml.NodeSeq
import io.finch._

trait CompleteMultipartUploadSupport {
  object CompleteMultipartUpload {
    val matcher = post(string / string ?
      param("uploadId") ?
      RequestId.reader ?
      CallerId.reader
    ).as[t]
    val endpoint = matcher { a: t => a.run }
    case class t(bucketName: String, keyName: String,
                 uploadId: String,
                 requestId: String, callerId: String) extends Task[Output[Future[NodeSeq]]] with Reportable {
      def resource = Resource.forObject(bucketName, keyName)
      def runOnce = {
        Ok(Future(NodeSeq.Empty))
      }
    }
  }
}
