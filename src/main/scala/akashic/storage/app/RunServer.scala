package akashic.storage.app

import java.nio.file.Files

import akashic.storage._
import akashic.storage.admin.TestUsers
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object RunServer extends App {
  val config = ServerConfig(ConfigFactory.load("run-server.conf"))
  Files.createDirectories(config.mountpoint)
  server = Server(config, cleanup = true)

  Await.ready(server.start, Duration.Inf)

  server.users.addUser(TestUsers.hoge)
  server.users.addUser(TestUsers.s3testsMain)
  server.users.addUser(TestUsers.s3testsAlt)
}
