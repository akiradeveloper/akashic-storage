package akashic.storage

import java.nio.file.{Path, Paths, Files}

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.StrictLogging

trait ServerConfig {
  def rawConfig: Config
  def ip: String
  def port: Int
  def adminPassword: String
}

object ServerConfig {
  def apply(configRoot: Config) = new ServerConfig {
    logger.info("configRoot: {}", configRoot)

    val rawConfig = configRoot.getConfig("akashic.storage")
    override def ip = rawConfig.getString("ip")
    override def port: Int = rawConfig.getInt("port")
    override def adminPassword = rawConfig.getString("admin-passwd")

    logger.info("config: {}", (ip, port, adminPassword))
  }
}
