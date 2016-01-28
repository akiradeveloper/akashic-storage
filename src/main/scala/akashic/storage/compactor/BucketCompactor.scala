package akashic.storage.compactor

import akashic.storage.patch.Bucket
import akashic.storage.Server
import scala.collection.mutable

case class BucketCompactor(unwrap: Bucket) extends Compactable {
  def compact = {
    val l = mutable.ListBuffer[Compactable]()
    l += PatchLogCompactor(unwrap.acl)
    l += PatchLogCompactor(unwrap.versioning)
    l ++= unwrap.listKeys.map(KeyCompactor(_))
    l.toSeq
  }
}
