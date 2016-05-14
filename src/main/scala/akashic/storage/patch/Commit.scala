package akashic.storage.patch

import java.nio.file._

import akashic.storage.backend.NodePath
import akashic.storage.server

object Commit {
  def replaceData[V, A](to: Data[V], makeData: NodePath => Data[V])(fn: Data[V] => A): A = {
    val (from, res) = server.astral.allocData(makeData, fn)
    server.astral.free(to)
    from.root.moveTo(to.root.dir, to.root.name)
    res
  }

  def once(to: DirectoryPath)(fn: DirectoryPath => Unit): Unit = {
    if (to.exists)
      return
    val (src, _) = server.astral.allocDirectory(fn)
    src.moveTo(to.dir, to.name)
  }

  def replaceDirectory[A](to: DirectoryPath)(fn: DirectoryPath => A): A = {
    val (from, res) = server.astral.allocDirectory(fn)
    // freeing should be after doing fn because fn may refer to the "to" resources
    server.astral.free(to)
    from.moveTo(to.dir, to.name)
    res
  }
}
