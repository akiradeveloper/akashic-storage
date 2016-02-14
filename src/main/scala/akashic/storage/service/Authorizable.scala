package akashic.storage.service

import akashic.storage.auth
import akashic.storage.auth.GetCallerId
import akashic.storage.server
import akka.http.scaladsl.model.HttpRequest

trait Authorizable[T] extends Runnable[T] with Error.Reportable {
  def resource: String
  def req: HttpRequest
  var callerId: String = ""
  abstract override def run = {
    val authKey = auth.authorize(resource, req)
    callerId = GetCallerId(authKey, requestId, resource).run
    super.run
  }
}
