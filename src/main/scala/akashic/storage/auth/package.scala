package akashic.storage

import akashic.storage.server
import akashic.storage.service.Error
import akka.http.scaladsl.model.HttpRequest

package object auth {
  def doGetSecretKey(accessKey: String): String = {
    val id = server.users.getId(accessKey).get
    server.users.find(id).get.secretKey
  }
  val getSecretKey: String => String = (accessKey: String) => doGetSecretKey(accessKey)
  def authorize(resource: String, req: HttpRequest): Option[String] = {
    val results: Seq[Option[String]] = Seq(V2.authorize(resource, req), V2Presigned.authorize(resource, req))
    if (results.forall(_ == Some(""))) return Some("")
    results.filterNot(_ == Some("")).filter(_.isDefined).headOption match {
      case Some(a) => Some(a.get)
      case None => None
    }
  }
}
