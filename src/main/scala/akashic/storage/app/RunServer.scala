package akashic.storage.app

import akashic.storage._

object RunServer extends App {
  server = Server(ServerConfig.forTest)
  server.start
}
