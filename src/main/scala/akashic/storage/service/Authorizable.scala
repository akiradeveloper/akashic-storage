package akashic.storage.service

import com.twitter.finagle.http.Request
import akashic.storage.auth

trait Authorizable extends Error.Reportable {
  def resource: String
  def req: Request
  var callerId: String = ""
  def authorize = {
    auth.authorize(resource, req) match {
      case Some(a) => callerId = a
      case None => failWith(Error.SignatureDoesNotMatch())
    }
  }
}
