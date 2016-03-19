package akashic.storage.admin

import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.Directives._
import akashic.storage.server

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
