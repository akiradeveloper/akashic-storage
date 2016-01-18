package akashic.storage.compactor

import akashic.storage.patch.Key
import akashic.storage.Server

case class KeyCompactor(unwrap: Key, server: Server) extends Compactable {
  def compact = {
    Seq()
  }
}
