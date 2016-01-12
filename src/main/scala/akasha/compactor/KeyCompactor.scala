package akasha.cleaner

import akasha.patch.Key
import akasha.Server

case class KeyCompactor(unwrap: Key, server: Server) extends Compactable {
  def compact = {
    Seq()
  }
}
