package akashic.storage.auth

import akashic.storage.admin.TestUsers
import com.twitter.finagle.http.Request

object V2 {
  def authorize(resource: String, request: Request): Option[String] =
    Some(TestUsers.hoge.id)
}
