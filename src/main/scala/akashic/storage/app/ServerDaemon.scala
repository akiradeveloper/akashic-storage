package akashic.storage.app

import akashic.storage.{Server, ServerConfig, server}
import akashic.storage.admin.TestUsers
import com.typesafe.config.ConfigFactory
import org.apache.commons.daemon.{DaemonContext, Daemon}
import java.io.File

class ServerDaemon extends Daemon {
  override def init(context: DaemonContext): Unit = {
    val config = ServerConfig(ConfigFactory.parseFile(new File("/opt/akashic-storage/etc/conf")))
    server = Server(config, cleanup = false)

    // this is a workaround before authorziation is available
    server.users.add(TestUsers.hoge)
  }
  override def start(): Unit = {
    server.start
  }
  override def stop(): Unit = {
    server.stop
  }
  override def destroy(): Unit = {}
}
