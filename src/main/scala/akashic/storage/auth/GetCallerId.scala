package akashic.storage.auth

import akashic.storage._
import akashic.storage.service.Error

case class GetCallerId(authKey: Option[String], requestId: String, resource: String) extends Error.Reportable {
  def run: String = {
    if (authKey.isEmpty) failWith(Error.SignatureDoesNotMatch())
    val res = authKey.get match {
      case "" => CallerId.ANONYMOUS
      case a => server.users.getId(a) match {
        case Some(id) => id
        case None => failWith(Error.AccountProblem())
      }
    }
    service.logger.debug(s"callerId: ${res}")
    res
  }
}
