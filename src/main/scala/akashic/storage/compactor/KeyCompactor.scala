package akashic.storage.compactor

import akashic.storage.patch.{Version, Key, Upload}
import akashic.storage.service.Meta
import akashic.storage.{files, Server}
import scala.collection.mutable

case class KeyCompactor(unwrap: Key, server: Server) extends Compactable {
  def result = {
    val l = mutable.ListBuffer[Compactable]()
    l ++= unwrap.versions.listPatches.map(_.asVersion).filter(_.committed).map(VersionCompactor(_, server))
    l ++= unwrap.uploads.listUploads.filter(_.committed).map(UploadCompactor(_, server))
    l.toSeq
  }

  def compact: Seq[Compactable] = {
    // remove committed uploads
    unwrap.uploads.listUploads.filter(_.committed).foreach { upload: Upload =>
      unwrap.findVersion(upload.reservedVersionId) match {
        case Some(a) => dispose(upload.root)
        case None =>
      }
    }

    val uploadings: Set[Int] = unwrap.uploads.listUploads
      .map(_.reservedVersionId)
      .toSet

    val maxIdCommitted = unwrap.versions.find match {
      case Some(a) => a.name.toInt
      case None => return result
    }

    unwrap.versions.listPatches.map(_.asVersion)
      .filter(_.committed)
      .filter(_.name.toInt < maxIdCommitted)
      // versioned object shouldn't be deleted implicitly
      .filterNot(a => Meta.fromBytes(a.meta.asData.readBytes).isVersioned)
      // uploads shouldn't lose their destinations
      .filterNot(a => uploadings.contains(a.name.toInt))
      .foreach(a => dispose(a.root))

    result
  }
}
