package akashic.storage.auth

import akashic.storage.HeaderList
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.digest.HmacUtils

case class V2Common(method: String, resource: String, paramList: ParamList.t, headerList: HeaderList.t) {
  // http://docs.aws.amazon.com/AmazonS3/latest/dev/RESTAuthentication.html
  val subresources = Set("cors", "acl", "lifecycle", "location", "logging", "notification", "partNumber", "policy", "requestPayment", "torrent", "uploadId", "uploads", "versionId", "versioning", "versions", "website")
  // response-* to override the response header
  val responseOverride = Set("response-content-type", "response-content-language", "response-expires", "response-cache-control", "response-content-disposition", "response-content-encoding")
  // and ?delete (for delete multiple objects)

  def computeSignature(stringToSign: String, secretKey: String): String = {
    Base64.encodeBase64String(HmacUtils.hmacSha1(secretKey.getBytes, stringToSign.getBytes("UTF-8")))
  }
  def stringToSign(dateOrExpire: String): String = {
    val contentType = headerList.find("Content-Type") match {
      case Some(a) => a.toLowerCase
      case None => ""
    }
    val cannonicalAmzHeaders: String = {
      headerList.unwrap
        .filter(_._1.toLowerCase.startsWith("x-amz-"))
        .sortBy(_._1)
        .map(a => s"${a._1.toLowerCase}:${a._2}\n")
        .mkString
    }
    val cannonicalResource: String = {
      val l = paramList.unwrap
        .sortBy(_._1)
        .filter { a =>
        val s = subresources ++ responseOverride ++ Set("delete")
        s.contains(a._1)
      }
      val params =
        if (l.isEmpty) {
          ""
        } else {
          "?" + l.map { a =>
            if (a._2 == "") {
              a._1
            } else {
              a._1 + "=" + a._2
            }
          }.mkString("&")
        }
      resource + params
    }
    val result = method + "\n" +
      headerList.find("Content-MD5").getOrElse("") + "\n" +
      headerList.find("Content-Type").getOrElse("").toLowerCase + "\n" +
      dateOrExpire + "\n" +
      cannonicalAmzHeaders +
      cannonicalResource
    println(result)
    result
  }
}
