package akashic.storage.compactor

import akashic.storage.patch.Part
import akashic.storage.Server

case class PartCompactor(unwrap: Part, server: Server) extends Compactable {
  def compact = {
    Seq(PatchLogCompactor(unwrap.versions, server))
  }
}
