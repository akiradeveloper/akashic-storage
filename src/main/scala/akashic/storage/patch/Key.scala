package akashic.storage.patch

import java.nio.file.{Files, Path}

case class Key(root: Path) extends Patch {
  val bucket = Bucket(root.getParent.getParent)
  val versions = Versions(root.resolve("versions"))
  val uploads = Uploads(root.resolve("uploads"))
  def init {
    Files.createDirectory(versions.root)
    Files.createDirectory(uploads.root)
  }
  def findLatestVersion: Option[Version] = {
    versions.findLatestVersion
  }
  def findVersion(id: Int): Option[Version] = {
    versions.findVersion(id)
  }
}
