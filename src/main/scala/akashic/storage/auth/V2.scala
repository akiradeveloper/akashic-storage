package akashic.storage.auth

import akashic.storage.HeaderList
import akashic.storage.admin.TestUsers
import com.twitter.finagle.http.Request

import scala.util.Try

object V2 {
  def authorize(resource: String, req: Request): Option[String] = {
    val headerList = HeaderList.fromRequest(req)
    val paramList = ParamList.fromRequest(req)
    Try {
      val authorization = headerList.find("Authorization").getOrElse("BANG!")
      val xs = authorization.split(" ")
      val a = xs(0)
      require(a == "AWS")
      val ys = xs(1).split(":")
      val accessKey = ys(0)
      require(accessKey != "")
      val signature = ys(1)
      require(signature != "")

      val secretKey = getSecretKey(accessKey)

      val date = {
        headerList.find("x-amz-date") match {
          case Some(a) => ""
          case None => headerList.find("Date").get
        }
      }
      val alg = V2Common(req.method.toString, resource, paramList, headerList)
      val stringToSign = alg.stringToSign(date)
      val computed = alg.computeSignature(stringToSign, secretKey)
      require(computed == signature)
      accessKey
    }.toOption
  }
}
