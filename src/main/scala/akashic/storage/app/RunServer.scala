package akashic.storage.app

import akashic.storage._
import akashic.storage.admin.TestUsers
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object RunServer extends App {
  server = Server(ServerConfig(ConfigFactory.load("test.conf"), init=true))

  // workaround
  server.users.addUser(TestUsers.hoge)

  // for s3-tests
  server.users.addUser(TestUsers.s3testsMain)
  server.users.addUser(TestUsers.s3testsAlt)

  Await.ready(server.start, Duration.Inf)
}
