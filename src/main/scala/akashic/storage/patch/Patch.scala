package akashic.storage.patch

import java.nio.file.{Files, Path}

import akashic.storage.backend.NodePath

object Patch {
  def apply(_root: NodePath) = new Patch { def root = _root }
}

trait Patch {
  def root: NodePath
  def name = root.name
}
