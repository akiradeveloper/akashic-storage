package akasha.service

import io.finch._
import akasha.Server
import akasha.service.Error.Reportable
import scala.xml.NodeSeq

trait GetBucketSupport {
  self: Server =>
  object GetBucket {
    val matcher = get(string ?
      paramOption("delimiter") ?
      paramOption("encoding-type") ?
      paramOption("marker") ?
      paramOption("max-keys") ?
      paramOption("prefix") ?
      RequestId.reader ?
      CallerId.reader
    ).as[t]
    val endpoint = matcher { a: t => a.run }
    case class t(bucketName: String,
                 delimiter: Option[String],
                 encodingType: Option[String],
                 marker: Option[String],
                 maxKeys: Option[String],
                 prefix: Option[String],
                 requestId: String,
                 callerId: String) extends Task[Output[NodeSeq]] with Reportable {
      def resource = Resource.forBucket(bucketName)
      def runOnce = {
        val xml = NodeSeq.Empty
        Ok(xml)
      }
    }
  }
}
