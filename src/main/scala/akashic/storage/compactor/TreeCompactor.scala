package akashic.storage.compactor

import akashic.storage.patch.Tree
import akashic.storage.Server

case class TreeCompactor(unwrap: Tree) extends Compactable {
  def compact = {
    unwrap.listBuckets.filter(_.committed).map(BucketCompactor(_))
  }
}
