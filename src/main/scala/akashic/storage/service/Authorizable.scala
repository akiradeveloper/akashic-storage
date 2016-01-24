package akashic.storage.service

import com.twitter.finagle.http.Request
import akashic.storage.auth
import akashic.storage.server

trait Authorizable extends Error.Reportable {
  def resource: String
  def req: Request
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
