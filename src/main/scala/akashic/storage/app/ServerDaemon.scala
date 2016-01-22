package akashic.storage.app

import akashic.storage.{Server, ServerConfig, server}
import com.typesafe.config.ConfigFactory
import org.apache.commons.daemon.{DaemonContext, Daemon}

class ServerDaemon extends Daemon {
  override def init(context: DaemonContext): Unit = {
    val config = ServerConfig(ConfigFactory.load("/opt/akashic-storage/etc/conf"), init=false)
    server = Server(config)
  }
  override def start(): Unit = {
    server.start
  }
  override def stop(): Unit = {
    server.stop
  }
  override def destroy(): Unit = {}
}
