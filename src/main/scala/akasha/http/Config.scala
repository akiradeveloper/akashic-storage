package akasha.http

import java.nio.file.{Files, Path, Paths}

import com.typesafe.config.{Config, ConfigFactory}

trait ServerConfig {
  def mountpoint: Path
  def ip: String
  def port: Int

  def treePath: Path = mountpoint.resolve("tree")
  def adminPath: Path = mountpoint.resolve("admin")
}

object ServerConfig {
  def forTest = {
    val configRoot = ConfigFactory.load
    fromConfig(configRoot)
  }

  def fromConfig(configRoot: Config) = new ServerConfig {
    val config = configRoot.getConfig("akasha")

    val mp = Paths.get(config.getString("mountpoint"))
    if (Files.exists(mp)) {
      akasha.Files.purgeDirectory(mp)
    }
    Files.createDirectory(mp)

    Files.createDirectory(treePath)
    Files.createDirectory(adminPath)

    override def mountpoint = mp
    override def ip = config.getString("ip")
    override def port: Int = config.getInt("port")
  }
}
