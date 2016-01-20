package akashic.storage.service

import io.finch.RequestReader

object CallerId {
  val reader: RequestReader[String] = akashic.storage.auth.reader
}
