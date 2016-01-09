package akasha.patch

import java.nio.file.{Files, Path}

import akasha.FileOps

case class Bucket(root: Path) extends Patch {
  val acl = PatchLog(root.resolve("acl"))
  val cors = PatchLog(root.resolve("cors"))
  val versioning = PatchLog(root.resolve("versioning"))
  val keys: Path = root.resolve("keys")
  def keyPath(name: String): Path = keys.resolve(name)
  def init {
    acl.init
    cors.init
    versioning.init
    Files.createDirectory(keys)
  }
  def findKey(name: String): Option[Key] = {
    val path = keys.resolve(name)
    if (Files.exists(path) && Key(path).committed) {
      Some(Key(path))
    } else {
      None
    }
  }
  def listKeys: Seq[Key] = {
    FileOps.children(keys).map(Key(_)).filter(_.committed)
  }
}
