package akashic.storage.patch

import akashic.storage.backend.NodePath

trait Patch {
  def root: NodePath
  def name = root.name
}
