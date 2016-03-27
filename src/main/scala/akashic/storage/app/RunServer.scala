package akashic.storage.app

import java.nio.file.{Paths, Files}

import akashic.storage._
import akashic.storage.admin.TestUsers
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object RunServer extends App {
  // TODO impl --loglevel= option
  val config = ServerConfig(ConfigFactory.load("run-server.conf"))
  val mountpoint = Paths.get(config.rawConfig.getConfig("backend").getString("mountpoint"))
  Files.createDirectories(mountpoint)
  server = Server(config, cleanup = true)

  val fut = server.start
  Await.ready(fut, Duration.Inf)

  server.users.add(TestUsers.hoge)
  server.users.add(TestUsers.s3testsMain)
  server.users.add(TestUsers.s3testsAlt)
}
