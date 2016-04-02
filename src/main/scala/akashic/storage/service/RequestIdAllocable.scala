package akashic.storage.service

import akashic.storage.strings

trait RequestIdAllocable extends Runnable {
  var requestId = ""
  abstract override def run = {
    requestId = strings.random(16)
    super.run
  }
}
