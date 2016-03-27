package akashic.storage.patch

import akashic.storage.backend.NodePath
import akashic.storage.service.{Location, Versioning, Acl}

case class Bucket(root: NodePath) extends Patch {
  val acl = Acl.makeCache(root.resolve("acl"))
  val versioning = Versioning.makeCache(root.resolve("versioning"))
  val location = Location.makeCache(root.resolve("location"))
  val keys: NodePath = root.resolve("keys")
  def keyPath(name: String): NodePath = keys.resolve(name)
  def init {
    keys.makeDir
  }
  def findKey(name: String): Option[Key] = {
    val path = keys.resolve(name)
    if (path.exists)
      Some(Key(this, path))
    else
      None
  }
  def listKeys: Iterable[Key] = keys.listDir.map(Key(this, _))
}
