package akashic.storage.service

import akashic.storage.auth
import akashic.storage.server
import akka.http.scaladsl.model.HttpRequest

trait Authorizable extends Error.Reportable {
  def resource: String
  def req: HttpRequest
  var callerId: String = ""
  def authorize = {
    auth.authorize(resource, req) match {
      case Some(a) =>
        println(s"auth OK: ${a}")
        callerId = a
      case None =>
        println("auth NG")
        failWith(Error.SignatureDoesNotMatch())
    }
  }
}
