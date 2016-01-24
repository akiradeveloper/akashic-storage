package akashic.storage.auth

import akashic.storage.HeaderList
import com.twitter.finagle.http.Request

import scala.util.Try

object V2Presigned {
  def authorize(resource: String, req: Request): Option[String] = Try {
    val headerList = HeaderList.fromRequest(req)
    val paramList = ParamList.fromRequest(req)
    val accessKey = paramList.find("AWSAccessKeyId").get
    val expires = paramList.find("Expires").get
    val signature = paramList.find("Signature").get
    val alg = V2Common(req.method.toString, resource, paramList, headerList)
    val stringToSign = alg.stringToSign(expires)
    val computed = alg.computeSignature(stringToSign, getSecretKey(accessKey))
    require(computed == signature)
    accessKey
  }.toOption
}
