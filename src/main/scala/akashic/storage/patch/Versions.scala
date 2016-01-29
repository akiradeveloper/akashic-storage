package akashic.storage.patch

import java.nio.file.{Files, Path}

import akashic.storage.files

case class Versions(root: Path) {
  def key = Key(root.getParent)
  def acquireWriteDest: Path = {
    // versioning disabled
    root.resolve("0")
  }
  private def acquireNewLoc: Path = {
    val xs = files.children(root).map(files.basename(_).toInt)
    val newId = if (xs.isEmpty) {
      1
    } else {
      xs.max + 1
    }
    root.resolve(newId.toString)
  }
  def listVersions: Seq[Version] = {
    files.children(root).map(Version(_)).sortBy(-1 * _.name.toInt)
  }
  def versionPath(id: Int): Path = root.resolve(id.toString)
  def findLatestVersion: Option[Version] = {
    listVersions.headOption
  }
  def findVersion(id: Int): Option[Version] = {
    val a = Version(versionPath(id))
    if (Files.exists(a.root)) {
      Some(a)
    } else None
  }
}
