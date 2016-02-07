package akashic.storage.auth

import akashic.storage.HeaderList
import akka.http.scaladsl.model.{ContentTypes, HttpRequest}
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.digest.HmacUtils

case class V2Common(req: HttpRequest, resource: String, paramList: ParamList.t, headerList: HeaderList.t) {
  val method = req.method.name

  // http://docs.aws.amazon.com/AmazonS3/latest/dev/RESTAuthentication.html
  val subresources = Set("cors", "acl", "lifecycle", "location", "logging", "notification", "partNumber", "policy", "requestPayment", "torrent", "uploadId", "uploads", "versionId", "versioning", "versions", "website")
  // response-* to override the response header
  val responseOverride = Set("response-content-type", "response-content-language", "response-expires", "response-cache-control", "response-content-disposition", "response-content-encoding")
  // and ?delete (for delete multiple objects)

  def computeSignature(stringToSign: String, secretKey: String): String = {
    Base64.encodeBase64String(HmacUtils.hmacSha1(secretKey.getBytes, stringToSign.getBytes("UTF-8")))
  }
  def stringToSign(dateOrExpire: String): Stream[String] = {
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

    // Workaround:
    // - mediaType.value
    // akka-http appends charset to the Content-Type
    // when the request from client lacks it.
    // e.g. text/plain -> text/plain; charset=UTF-8
    // This corrupts S3 authentication scheme so as the workaround
    // we check with both w/ or wo charset
    // - "" (empty string)
    // even though the client doesn't specify the Content-Type
    // akka-http sometimes infer the Content-Type as octet-stream
    val contentTypes = req.entity.contentType match {
      case ContentTypes.NoContentType => Stream("", "", "")
      case a => Stream(a.value, a.mediaType.value, "")
    }
    contentTypes map { contentType: String =>
      val result =
        method + "\n" +
        headerList.find("Content-Md5").getOrElse("") + "\n" +
        contentType.toLowerCase + "\n" +
        dateOrExpire + "\n" +
        cannonicalAmzHeaders +
        cannonicalResource
      // println("StringToSign:")
      // println(result)
      result
    }
  }
}
