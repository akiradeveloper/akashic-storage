package akashic.storage.auth

import akashic.storage.HeaderList
import akashic.storage.admin.TestUsers
import com.twitter.finagle.http.Request

import scala.util.Try

object V2 {
  def authorize(resource: String, req: Request): Option[String] = {
    doAuthorize(
      req.method.toString, 
      resource,
      ParamList.fromRequest(req),
      HeaderList.fromRequest(req),
      getSecretKey
    )
  }
  def doAuthorize(method: String, resource: String, paramList: ParamList.t, headerList: HeaderList.t, getSecretKeyFn: String => String): Option[String] = {
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

      val date = {
        headerList.find("x-amz-date") match {
          case Some(a) => ""
          case None => headerList.find("Date").get
        }
      }
      val alg = V2Common(method, resource, paramList, headerList)
      val stringToSign = alg.stringToSign(date)
      val computed = alg.computeSignature(stringToSign, getSecretKeyFn(accessKey))
      require(computed == signature)
      accessKey
    }.toOption
  }
}
