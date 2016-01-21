package akashic.storage.compactor

import akashic.storage.patch.Part
import akashic.storage.server

case class PartCompactor(unwrap: Part) extends Compactable {
  def compact = {
    Seq(PatchLogCompactor(unwrap.versions))
  }
}
