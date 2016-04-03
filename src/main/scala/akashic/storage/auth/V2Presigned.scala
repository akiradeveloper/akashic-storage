package akashic.storage.auth

import akashic.storage.HeaderList
import akka.http.scaladsl.model.HttpRequest

import scala.util.Try

object V2Presigned {
  def authorize(resource: String, req: HttpRequest): Option[String] = {
    val paramList = ParamList.fromRequest(req)
    val accessKeyParam: Option[String] = paramList.find("AWSAccessKeyId")
    if (accessKeyParam.isEmpty) return Some("")

    val headerList = HeaderList.fromRequest(req)

    Try {
      val accessKey = accessKeyParam.get
      require(accessKey != "")
      val expires = paramList.find("Expires").get
      require(expires != "")
      val signature = paramList.find("Signature").get
      require(signature != "")
      val alg = V2Common(req, resource, paramList, headerList)
      val stringToSign = alg.stringToSign(expires)
      val computed = stringToSign.map(alg.computeSignature(_, getSecretKey(accessKey)))
      if (computed.exists(_ == signature)) {
        accessKey
      } else {
        stringToSign.foreach(logger.error(_))
        assert(false)
        ""
      }
    }.toOption
  }
}
