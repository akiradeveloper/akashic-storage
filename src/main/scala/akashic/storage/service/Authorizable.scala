package akashic.storage.service

import akashic.storage.auth
import akashic.storage.auth.GetCallerId
import akashic.storage.server
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.HttpRequest

trait Authorizable extends Runnable {
  def requestId: String
  def resource: String
  var callerId: String = ""
  abstract override def run = {
    authorizeS3v2(resource, requestId) { ci =>
      callerId = ci
      super.run
    }
  }
}
