package fss3

import java.nio.file.{Paths, Path}

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
    val config = configRoot.getConfig("akka.s3")

    val mp = Paths.get(config.getString("mountpoint"))
    mp.mkdirp
    mp.emptyDirectory

    treePath.mkdirp
    adminPath.mkdirp

    override def mountpoint = mp
    override def ip = config.getString("ip")
    override def port: Int = config.getInt("port")
  }
}
