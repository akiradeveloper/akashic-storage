package akashic.storage.patch

import java.nio.file.{Files, Path}

case class Key(root: Path) extends Patch {
  val versions = PatchLog(root.resolve("versions"))
  val uploads = Uploads(root.resolve("uploads"))
  override def init {
    Files.createDirectory(versions.root)
    Files.createDirectory(uploads.root)
  }
  def findLatestVersion: Option[Version] = {
    versions.get.map(_.asVersion)
  }
  def findVersion(id: Int): Option[Version] = {
    versions.get(id).map(_.asVersion)
  }
}
