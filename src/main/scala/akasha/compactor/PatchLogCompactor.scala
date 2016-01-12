package akasha.cleaner

import akasha.patch.PatchLog
import akasha.Server

case class PatchLogCompactor(unwrap: PatchLog, server: Server) extends Compactable {
  def compact = {
    Seq()
  }
}
