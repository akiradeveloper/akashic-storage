package akashic.storage.compactor

import akashic.storage.patch.{Patch, PatchLog}
import akashic.storage.Server
import akashic.storage.files

case class PatchLogCompactor(unwrap: PatchLog, server: Server) extends Compactable {
  override def compact: Seq[Compactable] = {
    val maxIdCommitted = unwrap.find match {
      case Some(a) => a.name.toInt
      case None => return Seq()
    }
    files.children(unwrap.root)
      .map(Patch(_))
      .filter(_.committed)
      .filter(_.name.toInt < maxIdCommitted)
      .foreach(a => dispose(a.root))
    Seq()
  }
}
