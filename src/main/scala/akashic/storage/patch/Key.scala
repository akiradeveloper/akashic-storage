package akashic.storage.patch

import akashic.storage.backend.NodePath

case class Key(bucket: Bucket, root: NodePath) extends Patch {
  val versions = Versions(this, root("versions"))
  val uploads = Uploads(root("uploads"))
  def init {
    versions.root.makeDirectory
    uploads.root.makeDirectory
  }
  def findLatestVersion: Option[Version] = {
    versions.findLatestVersion
  }
  def findVersion(id: Int): Option[Version] = {
    versions.findVersion(id)
  }
  def deletable: Boolean = {
    versions.listVersions.forall(_.deletable)
  }
}
