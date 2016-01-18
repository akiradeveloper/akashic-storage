package akashic.storage.compactor

import akashic.storage.patch.Key
import akashic.storage.Server
import scala.collection.mutable

case class KeyCompactor(unwrap: Key, server: Server) extends Compactable {
  def compact = {
    val l = mutable.ListBuffer[Compactable]()
    // TODO
    l.toSeq
  }
}
