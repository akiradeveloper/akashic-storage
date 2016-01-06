package akasha.patch

import java.nio.file.Path

case class Key(root: Path) extends Patch {
  val versions = PatchLog(root.resolve("versions"))
  val uploads = Uploads(root.resolve("uploads"))
  def init {
    versions.init
    uploads.init
  }
  def findLatestVersion: Option[Version] = {
    versions.get.map(_.asVersion)
  }
  def findVersion(id: Int): Option[Version] = {
    versions.get(id).map(_.asVersion)
  }
}
