package akashic.storage.service

import akashic.storage.service.Error.Reportable
import io.finch._

import scala.xml.NodeSeq

trait ListPartsSupport {
  object ListParts {
    val matcher = get(string / string / paramExists("uploadId") ?
      param("uploadId") ?
      RequestId.reader ?
      CallerId.reader).as[t]
    val endpoint = matcher { a: t => a.run }
    case class t(bucketName: String, keyName: String,
                 uploadId: String,
                 requestId: String, callerId: String) extends Task[Output[NodeSeq]] with Reportable {
      def name = "List Parts"
      def resource = Resource.forObject(bucketName, keyName)
      def runOnce = {
        Ok(NodeSeq.Empty)
      }
    }
  }
}
