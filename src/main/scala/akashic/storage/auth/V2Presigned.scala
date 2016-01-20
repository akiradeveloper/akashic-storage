package akashic.storage.auth

import com.twitter.finagle.http.Request
import io.finch._

object V2Presigned {
  val reader: RequestReader[Option[String]] = RequestReader { req: Request =>
    None
  }
}
