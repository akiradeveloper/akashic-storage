package akashic.storage.compactor

import akashic.storage.patch.Tree
import akashic.storage.Server

case class TreeCompactor(unwrap: Tree) extends Compactable {
  def compact = {
    unwrap.listBuckets.map(BucketCompactor(_))
  }
}
