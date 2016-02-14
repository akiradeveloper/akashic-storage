package akashic.storage.auth

import akashic.storage.HeaderList
import akashic.storage.admin.TestUsers
import akka.http.scaladsl.model.HttpRequest

import scala.util.Try

object V2 {
  def authorize(resource: String, req: HttpRequest): Option[String] = {
    val paramList = ParamList.fromRequest(req)
    val headerList = HeaderList.fromRequest(req)

    val authHeader: Option[String] = headerList.find("Authorization")
    if (authHeader.isEmpty) return Some("")

    Try {
      val authorization = authHeader.get
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
      val alg = V2Common(req, resource, paramList, headerList)
      val stringToSign = alg.stringToSign(date)
      val computed = stringToSign.map(alg.computeSignature(_, getSecretKey(accessKey)))
      require(computed.exists(_ == signature))
      accessKey
    }.toOption
  }
}
