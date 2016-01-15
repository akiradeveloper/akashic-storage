package akashic.storage.cleaner

import akashic.storage.patch.Tree
import akashic.storage.Server

case class TreeCompactor(tree: Tree, server: Server) extends Compactable {
  def compact = {
    Seq()
  }
}
