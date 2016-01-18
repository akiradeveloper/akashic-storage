package akashic.storage.compactor

import akashic.storage.patch.Bucket
import akashic.storage.Server
import scala.collection.mutable

case class BucketCompactor(unwrap: Bucket, server: Server) extends Compactable {
  def compact = {
    val l = mutable.ListBuffer[Compactable]()
    l += PatchLogCompactor(unwrap.acl, server)
    l += PatchLogCompactor(unwrap.versioning, server)
    l ++= unwrap.listKeys.map(KeyCompactor(_, server))
    l.toSeq
  }
}
