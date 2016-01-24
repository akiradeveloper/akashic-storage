package akashic.storage.auth

import akashic.storage.HeaderList
import com.twitter.finagle.http.Request

import scala.util.Try

object V2Presigned {
  def authorize(resource: String, req: Request): Option[String] = {
    doAuthorize(req.method.toString, resource, ParamList.fromRequest(req), HeaderList.fromRequest(req), getSecretKey)
  }
  def doAuthorize(method: String, resource: String, paramList: ParamList.t, headerList: HeaderList.t, getSecretKeyFn: String => String): Option[String] = {
    Try {
      val accessKey = paramList.find("AWSAccessKeyId").get
      require(accessKey != "")
      val expires = paramList.find("Expires").get
      require(expires != "")
      val signature = paramList.find("Signature").get
      require(signature != "")
      val alg = V2Common(method, resource, paramList, headerList)
      val stringToSign = alg.stringToSign(expires)
      val computed = alg.computeSignature(stringToSign, getSecretKeyFn(accessKey))
      require(computed == signature)
      accessKey
    }.toOption
  }
}
