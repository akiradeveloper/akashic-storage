package akashic.storage.patch

import java.nio.file.Path

import akashic.storage.service.Meta

case class Version(root: Path) extends Patch {
  def key = Key(root.getParent.getParent)
  val data = Data(root.resolve("data"))
  val meta = Data(root.resolve("meta"))
  val acl = Data(root.resolve("acl"))

  // [spec] All objects (including all object versions and delete markers)
  // in the bucket must be deleted before the bucket itself can be deleted.
  val deletable = false
}
