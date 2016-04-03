package akashic.storage.admin

import akashic.storage.server
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.Credentials

object Auth {
  def authenticator(credentials: Credentials): Option[String] = {
    credentials match {
      case p @ Credentials.Provided(id) if p.verify(server.config.adminPassword) => Some(id)
      case _ =>
        logger.error("credentials ({}) not authenticated", credentials)
        None
    }
  }
  val authenticate = authenticateBasic(realm = "akashic-storage-admin", authenticator).tflatMap(_ => pass)
}
