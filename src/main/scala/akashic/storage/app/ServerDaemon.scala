package akashic.storage.app

import akashic.storage.{Server, ServerConfig, server}
import akashic.storage.admin.TestUsers
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.core.util.StatusPrinter
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.commons.daemon.{DaemonContext, Daemon}
import java.io.File

import org.slf4j.LoggerFactory

import scala.concurrent.Await

class ServerDaemon extends Daemon {
  override def init(context: DaemonContext): Unit = {
    println("init daemon")

    val lc = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    StatusPrinter.print(lc)

    val config = ServerConfig(
        ConfigFactory.parseFile(new File("/opt/akashic-storage/etc/application.conf"))
        .withFallback(ConfigFactory.load()))
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
