package akashic.storage.app

import akashic.storage.{Server, ServerConfig, server}
import akashic.storage.admin.TestUsers
import com.typesafe.config.ConfigFactory
import org.apache.commons.daemon.{DaemonContext, Daemon}
import java.io.File

import scala.concurrent.Await

class ServerDaemon extends Daemon {
  override def init(context: DaemonContext): Unit = {
    val config = ServerConfig(ConfigFactory.load("server-daemon.conf"))
    server = Server(config, cleanup = false)
  }
  override def start(): Unit = {
    server.start
  }
  override def stop(): Unit = {
    server.stop
  }
  override def destroy(): Unit = {}
}
