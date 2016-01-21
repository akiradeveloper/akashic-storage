package akashic.storage.compactor

import akashic.storage.patch.Version
import akashic.storage.Server

case class VersionCompactor(unwrap: Version) extends Compactable {
  def compact = {
    Seq(PatchLogCompactor(unwrap.acl))
  }
}
