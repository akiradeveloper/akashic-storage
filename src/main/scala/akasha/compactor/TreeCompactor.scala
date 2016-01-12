package akasha.cleaner

import akasha.patch.Tree
import akasha.Server

case class TreeCompactor(tree: Tree, server: Server) extends Compactable {
  def compact = {
    Seq()
  }
}
