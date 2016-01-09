package akasha

import java.nio.file.{Path, Paths, Files}

import com.typesafe.config.{Config, ConfigFactory}

trait ServerConfig {
  def mountpoint: Path
  def ip: String
  def port: Int

  def treePath: Path = mountpoint.resolve("tree")
  def adminPath: Path = mountpoint.resolve("admin")
}

object ServerConfig {
  def forProduction = ???

  def forTest = {
    val configRoot = ConfigFactory.load
    forConfig(configRoot)
  }

  def forConfig(configRoot: Config) = new ServerConfig {
    val config = configRoot.getConfig("akasha")

    val mp = Paths.get(config.getString("mountpoint"))
    if (Files.exists(mp)) {
      files.purgeDirectory(mp)
    }
    Files.createDirectory(mp)

    Files.createDirectory(treePath)
    Files.createDirectory(adminPath)

    override def mountpoint = mp
    override def ip = config.getString("ip")
    override def port: Int = config.getInt("port")
  }
}
