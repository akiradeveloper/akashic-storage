package akashic.storage

import com.twitter.finagle.http.Request
import akashic.storage.server

package object auth {
  def doGetSecretKey(accessKey: String): String = {
    val id = server.users.getId(accessKey).get
    server.users.getUser(id).get.secretKey
  }
  val getSecretKey: String => String = (accessKey: String) => doGetSecretKey(accessKey)
  def authorize(resource: String, req: Request): Option[String] =
    Seq(V2.authorize(resource, req), V2Presigned.authorize(resource, req)).find(_.isDefined).flatten
}
