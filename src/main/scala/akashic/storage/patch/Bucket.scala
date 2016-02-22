package akashic.storage.patch

import java.nio.file.{Files, Path}

import akashic.storage.files

case class Bucket(root: Path) extends Patch {
  val acl = Data(root.resolve("acl"))
  val versioning = Data(root.resolve("versioning"))
  val location = Data(root.resolve("location"))
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
