package akashic.storage.compactor

import akashic.storage.patch.{Version, Key, Upload}
import akashic.storage.service.Meta
import akashic.storage.{files, server}
import scala.collection.mutable

case class KeyCompactor(unwrap: Key) extends Compactable {
  def result = {
    val l = mutable.ListBuffer[Compactable]()
    l ++= unwrap.versions.listVersions.map(VersionCompactor(_))
    l ++= unwrap.uploads.listUploads.map(UploadCompactor(_))
    l.toSeq
  }

  def compact: Seq[Compactable] = {
    val maxIdCommitted = unwrap.versions.findLatestVersion match {
      case Some(a) => a.name.toInt
      case None => return result
    }

    unwrap.versions.listVersions
      .filter(_.name.toInt < maxIdCommitted)
      // versioned object shouldn't be deleted implicitly
      .filterNot(a => Meta.fromBytes(a.meta.asData.readBytes).isVersioned)
      .foreach(a => dispose(a.root))

    result
  }
}
