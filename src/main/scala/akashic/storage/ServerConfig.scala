package akashic.storage

import com.typesafe.config.Config

case class ServerConfig(
  rawConfig: Config,
  ip: String,
  port: Int,
  adminPassword: String
)

object ServerConfig {
  def fromConfig(configRoot: Config) = {
    logger.info("configRoot: {}", configRoot)

    val rawConfig = configRoot.getConfig("akashic.storage")
    val ip = rawConfig.getString("ip")
    val port: Int = rawConfig.getInt("port")
    val adminPassword = rawConfig.getString("admin-passwd")

    logger.info("config: {}", (ip, port, adminPassword))

    ServerConfig(rawConfig, ip, port, adminPassword)
  }
}
