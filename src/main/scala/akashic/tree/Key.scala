package akashic.store

import java.nio.file.Path

import scala.util.{Random, Try}

case class Key(parent: Bucket, path: Path) {
  val versions: Versions = path.resolve("versions")

  def listVersions: Seq[Version] = {
    versions.children
      .map(Version(this, _))
      .sortWith(_.id > _.id)
  }

  def getCurrentVersion: Option[Version] = {
    if (ov.isEmpty) {
      None
    } else {
      val v = ov.get
      if (!v.metaT.isVersioned && v.metaT.isDeleteMarker) {
        None
      } else {
        ov
      }
    }
  }

  def acquireNewVersion: Version = {
    val latestId = listVersions.headOption match {
      case Some(a) => new Version(this, a).id
      case None => 0
    }
    val newVer = new Version(this, versions.resolve((latestId + 1).toString))
    newVer.mk
    newVer
  }
}

