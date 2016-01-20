package akashic.storage.auth

import akashic.storage.admin.TestUsers
import com.twitter.finagle.http.Request
import io.finch._

object V2 {
  val reader: RequestReader[Option[String]] = RequestReader { req: Request =>
    Some(TestUsers.hoge.id)
  }
}
