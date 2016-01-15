package akashic.storage.cleaner

import akashic.storage.patch.PatchLog
import akashic.storage.Server

case class PatchLogCompactor(unwrap: PatchLog, server: Server) extends Compactable {
  def compact = {
    Seq()
  }
}
