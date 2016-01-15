package akashic.storage.service

import akashic.storage.strings
import io.finch.RequestReader

object RequestId {
  val reader = RequestReader.value(strings.random(16))
}
