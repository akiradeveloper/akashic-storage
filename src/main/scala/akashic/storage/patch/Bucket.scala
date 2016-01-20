package akashic.storage.patch

import java.nio.file.{Files, Path}

import akashic.storage.files

case class Bucket(root: Path) extends Patch {
  val acl = PatchLog(root.resolve("acl"))
  val versioning = PatchLog(root.resolve("versioning"))
  val keys: Path = root.resolve("keys")
  def keyPath(name: String): Path = keys.resolve(name)
  override def init {
    Files.createDirectory(acl.root)
    Files.createDirectory(versioning.root)
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
  def listKeys: Seq[Key] = files.children(keys).map(Key(_))
}
