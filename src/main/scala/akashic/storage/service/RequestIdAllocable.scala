package akashic.storage.service

import akashic.storage.strings

trait RequestIdAllocable {
  var requestId = ""
  def allocRequestId = {
    requestId = strings.random(16)
  }
}
