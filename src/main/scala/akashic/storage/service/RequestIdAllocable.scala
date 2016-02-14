package akashic.storage.service

import akashic.storage.strings

trait RequestIdAllocable[T] extends Runnable[T] {
  var requestId = ""
  abstract override def run = {
    requestId = strings.random(16)
    super.run
  }
}
