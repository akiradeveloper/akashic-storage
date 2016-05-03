package akashic.storage.patch

import akashic.storage.backend.NodePath
import akashic.storage.service.{Acl, Location, Versioning}

case class Bucket(root: NodePath) extends Patch {
  val acl = Acl.makeCache(root("acl"))
  val versioning = Versioning.makeCache(root("versioning"))
  val location = Location.makeCache(root("location"))
  val keys: NodePath = root("keys")
  def keyPath(name: String): NodePath = keys(name)
  def init {
    keys.makeDirectory
  }
  def findKey(name: String): Option[Key] = {
    val path = keys(name)
    if (path.exists)
      Some(Key(this, path))
    else
      None
  }
  def listKeys: Iterable[Key] = keys.listDir.map(Key(this, _))
  // Since there is no API to change the location of existing bucket
  // and there is no chance that location file isn't created.
  // We can use the creation time as the creation time of the bucket.
  def creationTime: Long = location.root.getAttr.creationTime
}
