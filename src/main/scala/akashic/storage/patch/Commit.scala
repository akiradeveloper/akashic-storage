package akashic.storage.patch

import java.nio.file._

import akashic.storage.backend.NodePath
import akashic.storage.server

object Commit {
  // for file
  def replaceData[V, A](to: Data[V], makeTemp: NodePath => Data[V])(fn: Data[V] => A): A = {
    val (from, res) = server.astral.allocData(makeTemp, fn)
    from.root.moveTo(to.root.dir, to.root.name, replaceIfExists = true)
    res
  }

  // for directory
  def once(to: DirectoryPath)(fn: DirectoryPath => Unit): Unit = {
    if (to.exists)
      return
    val (src, _) = server.astral.allocDirectory(fn)
    src.moveTo(to.dir, to.name, replaceIfExists = false)
  }

  def replaceDirectory[A](to: DirectoryPath)(fn: DirectoryPath => A): A = {
    server.astral.free(to)
    val (from, res) = server.astral.allocDirectory(fn)
    from.moveTo(to.dir, to.name, replaceIfExists = false)
    res
  }
}
