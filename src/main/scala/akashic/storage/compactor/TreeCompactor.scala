package akashic.storage.compactor

import akashic.storage.patch.Tree
import akashic.storage.Server

case class TreeCompactor(unwrap: Tree, server: Server) extends Compactable {
  def compact = {
    unwrap.listBuckets.map(BucketCompactor(_, server))
  }
}
