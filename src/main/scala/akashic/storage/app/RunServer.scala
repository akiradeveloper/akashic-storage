package akashic.storage.app

import akashic.storage._
import com.typesafe.config.ConfigFactory

object RunServer extends App {
  server = Server(ServerConfig(ConfigFactory.load, init=true))
  server.start
}
