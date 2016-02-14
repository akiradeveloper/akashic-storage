package akashic.storage.service

import akka.http.scaladsl.server.Directives._

object PostObject {
  val matcher =
    post &
      extractBucket &
      formFields(
        "key",
        "file".as[Array[Byte]],
        "acl" ?,
        "Content-Type" ?,
        "Content-Disposition" ?
      )
  val route = matcher.as(t)(_.run)

  case class t(bucketName: String, keyName: String,
               data: Array[Byte],
               acl: Option[String],
               contentType: Option[String],
               contentDisposition: Option[String]) extends AnonymousAPI {
    override def name = "POST Object"
    override def resource = Resource.forBucket(bucketName)
    override def runOnce = {
      complete("")
    }
  }
}
