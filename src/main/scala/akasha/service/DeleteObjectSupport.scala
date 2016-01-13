package akasha.service

import akasha.service.Error.Reportable
import io.finch._
import akasha.Server

trait DeleteObjectSupport {
  self: Server =>
  object DeleteObject {
    val matcher = delete(string / string ?
      paramOption("versionId").as[Int] ?
      RequestId.reader ?
      CallerId.reader
    ).as[t]
    val endpoint = matcher { a: t => a.run }
    case class t(bucketName: String, keyName: String,
                 versionId: Option[Int],
                 requestId: String,
                 callerId: String) extends Task[Output[Unit]] with Reportable {
      def resource = Resource.forObject(bucketName, keyName)
      def runOnce = {
        Ok()
      }
    }
  }
}

