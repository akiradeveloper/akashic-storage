package akashic.storage

import java.nio.file.{Path, Paths, Files}

import com.typesafe.config.{Config, ConfigFactory}

trait ServerConfig {
  def mountpoint: Path
  def ip: String
  def port: Int
}

object ServerConfig {
  def apply(configRoot: Config, init: Boolean) = new ServerConfig {
    val config = configRoot.getConfig("akashic.storage")

    val mp = Paths.get(config.getString("mountpoint"))
    if (init && Files.exists(mp)) {
      files.purgeDirectory(mp)
    }
    if (!Files.exists(mp)) {
      Files.createDirectory(mp)
    }

    override def mountpoint = mp
    override def ip = config.getString("ip")
    override def port: Int = config.getInt("port")
  }
}
