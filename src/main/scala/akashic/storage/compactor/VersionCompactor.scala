package akashic.storage.cleaner

import akashic.storage.patch.Version
import akashic.storage.Server

case class VersionCompactor(unwrap: Version, server: Server) extends Compactable {
  def compact = {
    Seq()
  }
}
