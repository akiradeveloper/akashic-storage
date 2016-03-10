package akashic.storage.patch

import java.nio.file.{Files, Path}

import akashic.storage.caching.{CacheMap, Cache}
import akashic.storage.files
import akashic.storage.service.{Location, Versioning, Acl}
import akashic.storage.service.Acl.t

case class Bucket(root: Path) extends Patch {
  val acl = Acl.makeCache(root.resolve("acl"))
  val versioning = Versioning.makeCache(root.resolve("versioning"))
  val location = Location.makeCache(root.resolve("location"))
  val keys: Path = root.resolve("keys")
  def keyPath(name: String): Path = keys.resolve(name)
  def init {
    Files.createDirectory(keys)
  }
  def findKey(name: String): Option[Key] = {
    val path = keys.resolve(name)
    if (Files.exists(path)) {
      Some(Key(path))
    } else {
      None
    }
  }
  def listKeys: Seq[Key] = files.children(keys).map(Key(_))
}
