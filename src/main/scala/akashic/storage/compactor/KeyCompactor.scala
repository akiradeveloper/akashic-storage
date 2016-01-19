package akashic.storage.compactor

import java.nio.file.Path
import akashic.storage.patch.{Version, Key, Upload}
import akashic.storage.service.Meta
import akashic.storage.{files, Server}
import scala.collection.mutable

case class KeyCompactor(unwrap: Key, server: Server) extends Compactable {
  def result = {
    val l = mutable.ListBuffer[Compactable]()
    l ++= files.children(unwrap.versions.root).map(Version(_)).filter(_.committed).map(VersionCompactor(_, server))
    l ++= files.children(unwrap.uploads.root).map(Upload(_)).filter(_.committed).map(UploadCompactor(_, server))
    l.toSeq
  }

  def compact: Seq[Compactable] = {
    // remove committed uploads
    files.children(unwrap.uploads.root).map(Upload(_)).filter(_.committed).foreach { upload: Upload =>
      val dest = unwrap.findVersion(upload.reservedVersionId).get
      if (dest.committed) {
        dispose(upload.root)
      }
    }

    val uploadings: Set[Int] = files.children(unwrap.uploads.root)
      .map(Upload(_))
      .map(_.reservedVersionId)
      .toSet

    val maxIdCommitted = unwrap.versions.find match {
      case Some(a) => a.name.toInt
      case None => return result
    }

    files.children(unwrap.versions.root).map(Version(_))
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
