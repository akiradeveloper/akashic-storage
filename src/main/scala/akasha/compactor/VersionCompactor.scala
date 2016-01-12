package akasha.cleaner

import akasha.patch.Version
import akasha.Server

case class VersionCompactor(unwrap: Version, server: Server) extends Compactable {
  def compact = {
    Seq()
  }
}
