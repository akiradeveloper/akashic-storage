package akashic.storage.patch

import akashic.storage.backend.NodePath
import akashic.storage.service.{Acl, Meta}

case class Version(key: Key, root: NodePath) extends Patch {
  val data = Data.Pure(root("data"))
  val meta = Meta.makeCache(root("meta"))
  val acl = Acl.makeCache(root("acl"))
  // [spec] All objects (including all object versions and delete markers)
  // in the bucket must be deleted before the bucket itself can be deleted.
  val deletable = false
  def versionId = meta.get.versionId
  def isDeleteMarker = meta.get.isDeleteMarker
  def getAttr = data.filePath.getAttr
}
