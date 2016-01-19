package akashic.storage.compactor

import java.nio.file.Path
import akashic.storage.patch.{Key, Upload}
import akashic.storage.{files, Server}
import scala.collection.mutable

case class KeyCompactor(unwrap: Key, server: Server) extends Compactable {
  def compact: Seq[Compactable] = {
    val l = mutable.ListBuffer[Compactable]()

    // remove committed uploads
    files.children(unwrap.uploads.root).map(Upload(_)).foreach { upload: Upload =>
      val dest = unwrap.findVersion(upload.reservedVersionId).get
      if (dest.committed) {
        dispose(upload.root)
      }
    }

    val uploadings: Seq[Int] = files.children(unwrap.uploads.root).map(Upload(_).reservedVersionId)

    val maxIdCommitted = unwrap.versions.find match {
      case Some(a) => a.name.toInt
      case None => return Seq() // ?
    }

    val committedPatches: Seq[Int] = unwrap.versions.listVersions.map(_.name.toInt)
    l.toSeq
  }
}
