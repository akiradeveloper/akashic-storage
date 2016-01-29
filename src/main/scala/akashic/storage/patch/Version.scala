package akashic.storage.patch

import java.nio.file.Path

import akashic.storage.service.Meta

case class Version(root: Path) extends Patch {
  def key = Key(root.getParent.getParent)
  val data = Data(root.resolve("data"))
  val meta = Data(root.resolve("meta"))
  val acl = Data(root.resolve("acl"))

  def deletable: Boolean = {
    val m = Meta.fromBytes(meta.readBytes)
    if (m.isDeleteMarker) return false
    if (m.isVersioned) return false
    true
  }
}
