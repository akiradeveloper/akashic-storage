package akashic.storage

import java.nio.file.{Path, Paths, Files}

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.StrictLogging

trait ServerConfig {
  def mountpoint: Path
  def ip: String
  def port: Int
  def adminPassword: String
}

object ServerConfig {
  def apply(configRoot: Config) = new ServerConfig {
    val config = configRoot.getConfig("akashic.storage")
    override def mountpoint = Paths.get(config.getString("mountpoint"))
    override def ip = config.getString("ip")
    override def port: Int = config.getInt("port")
    override def adminPassword = config.getString("admin-passwd")

    logger.info("config: {}", (mountpoint, ip, port, adminPassword))
  }
}
