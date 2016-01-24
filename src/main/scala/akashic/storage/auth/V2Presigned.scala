package akashic.storage.auth

import com.twitter.finagle.http.Request
import io.finch._

object V2Presigned {
  def authorize(resource: String, req: Request): Option[String] = None
}
