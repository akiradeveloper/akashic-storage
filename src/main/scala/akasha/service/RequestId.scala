package akasha.service

import akasha.strings
import io.finch.RequestReader

object RequestId {
  val reader = RequestReader.value(strings.random(16))
}
