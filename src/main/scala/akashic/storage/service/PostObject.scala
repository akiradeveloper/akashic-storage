package akashic.storage.service

import akashic.storage._
import akashic.storage.auth.{GetCallerId, V2Post}
import akka.http.scaladsl.server.Directives._

object PostObject {
  val matcher =
    post &
      extractBucket &
      formFields(
        "key",
        "file".as[Array[Byte]],
        "acl"?,
        "Content-Type"?,
        "Content-Disposition"?,
        "policy"?,
        "AWSAccessKeyId"?,
        "signature"?
      )
  val route = matcher.as(t)(_.run)

  case class t(bucketName: String, keyName: String,
               data: Array[Byte],
               acl: Option[String],
               contentType: Option[String],
               contentDisposition: Option[String],
               policy: Option[String],
               accessKey: Option[String],
               signature: Option[String]) extends AnonymousAPI {
    override def name = "POST Object"
    override def resource = Resource.forBucket(bucketName)
    override def runOnce = {
      val authKey: Option[String] = V2Post(policy, accessKey, signature).authorize
      val callerId = GetCallerId(authKey, requestId, resource).run
      complete("")
    }
  }
}
