package akashic.storage

import java.net.{URLEncoder, URLDecoder}

import akashic.storage.patch.Version
import scala.xml.NodeSeq
import akka.http.scaladsl.server.Directives._

package object service {
  // first appearance wins
  implicit class _Option[A](unwrap: Option[A]) {
    def `<+`(other: Option[A]): Option[A] = 
      unwrap match {
        case Some(a) => unwrap
        case None => other
      }
  }

  val X_AMZ_REQUEST_ID = "x-amz-request-id"
  val X_AMZ_VERSION_ID = "x-amz-version-id"
  val X_AMZ_DELETE_MARKER = "x-amz-delete-marker"

  def quoteString(raw: String): String = s""""${raw}""""

  def encodeKeyName(keyName: String): String = URLEncoder.encode(keyName, "UTF-8")
  def decodeKeyName(keyName: String): String = URLDecoder.decode(keyName, "UTF-8")
  val extractBucket = path(Segment ~ (Slash | PathEnd))
  val extractObject = path(Segment / Rest)
}
