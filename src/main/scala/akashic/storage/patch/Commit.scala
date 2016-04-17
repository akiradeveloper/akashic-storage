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
  def once(to: NodePath)(fn: Patch => Unit): Unit = {
    if (to.exists)
      return
    val (src, _) = server.astral.allocDirectory(fn)
    src.root.moveTo(to.dir, to.name, replaceIfExists = false)
  }

  def replaceDirectory[A](to: Patch)(fn: Patch => A): A = {
    server.astral.free(to)
    val (from, res) = server.astral.allocDirectory(fn)
    from.root.moveTo(to.root.dir, to.root.name, replaceIfExists = false)
    res
  }

  // not used
  def retry(alloc: () => NodePath)(fn: Patch => Unit): Patch = {
    def move(src: Patch): Patch = {
      val dest = Patch(alloc())
      try {
        src.root.moveTo(dest.root.dir, dest.root.name, replaceIfExists = false)
      } catch {
        case e: FileAlreadyExistsException =>
          move(src)
        case e: Throwable =>
          throw e
      }
      dest
    }
    val (src, _) = server.astral.allocDirectory(fn)
    move(src)
  }
}
