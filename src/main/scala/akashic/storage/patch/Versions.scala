package akashic.storage.patch

import akashic.storage.backend.NodePath

case class Versions(key: Key, root: NodePath) {
  def acquireWriteDest: NodePath = {
    // versioning disabled
    root("0")
  }
  private def acquireNewLoc: NodePath = {
    val xs = root.listDir.map(_.name.toInt)
    val newId = if (xs.isEmpty) {
      1
    } else {
      xs.max + 1
    }
    root(newId.toString)
  }
  def listVersions: Seq[Version] = {
    root.listDir.map(Version(key, _)).toSeq.sortBy(-1 * _.name.toInt)
  }
  def versionPath(id: Int) = root(id.toString)
  def findLatestVersion: Option[Version] = {
    listVersions.headOption
  }
  def findVersion(id: Int): Option[Version] = {
    val a = Version(key, versionPath(id))
    if (a.root.exists) {
      Some(a)
    } else None
  }
}
