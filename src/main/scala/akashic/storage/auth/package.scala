package akashic.storage

import akashic.storage.server
import akashic.storage.service.Error
import akka.http.scaladsl.model.HttpRequest
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

package object auth {
  val logger = Logger(LoggerFactory.getLogger("akashic.storage.auth"))
  def doGetSecretKey(accessKey: String): String = {
    val id = server.users.getId(accessKey) match {
      case Some(a) => a
      case None =>
        logger.error(s"no user for accessKey: ${accessKey}")
        assert(false)
        ""
    }
    server.users.find(id).get.secretKey
  }
  val getSecretKey: String => String = (accessKey: String) => doGetSecretKey(accessKey)
  def authorize(resource: String, req: HttpRequest): Option[String] = {
    val results: Seq[Option[String]] = Seq(V2.authorize(resource, req), V2Presigned.authorize(resource, req))
    if (results.forall(_ == Some(""))) return Some("")
    results.filterNot(_ == Some("")).filter(_.isDefined).headOption match {
      case Some(a) => Some(a.get)
      case None =>
        logger.error(s"failed to authorize: req=${req} db=${server.users.list}")
        None
    }
  }
}
