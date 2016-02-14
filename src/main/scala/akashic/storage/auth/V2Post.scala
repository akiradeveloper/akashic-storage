package akashic.storage.auth

import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.digest.HmacUtils

import scala.util.Try

case class V2Post(policy: Option[String], accessKey: Option[String], signature: Option[String]) {
  def authorize: Option[String] = {
    if (policy.isEmpty) return Some("")
    Try {
      val secretKey = getSecretKey(accessKey.get)
      val stringToSign = policy.get
      val arr: Array[Byte] = HmacUtils.hmacSha1(secretKey.getBytes, stringToSign.getBytes("UTF-8"))
      val computedSignature = Base64.encodeBase64String(arr)
      computedSignature == signature.get
      accessKey.get
    }.toOption
  }
}
