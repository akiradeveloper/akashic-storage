package akashic.storage.app

import akashic.storage._
import akashic.storage.admin.TestUsers
import com.typesafe.config.ConfigFactory

object RunServer extends App {
  server = Server(ServerConfig(ConfigFactory.load, init=true))
  server.start

  // for s3-tests
  server.users.addUser(TestUsers.s3testsMain)
  server.users.addUser(TestUsers.s3testsAlt)
}
