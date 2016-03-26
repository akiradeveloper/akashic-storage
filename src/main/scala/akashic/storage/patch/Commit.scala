package akashic.storage.patch

import java.nio.file._

import akashic.storage.backend.NodePath
import akashic.storage.caching.Cache
import akashic.storage.server

object Commit {
  // for file
  def replaceData[V](to: Data[V], makeTemp: NodePath => Data[V])(fn: Data[V] => Unit): Unit = {
    val from: Data[V] = server.astral.allocData(makeTemp, fn)
    from.root.moveTo(to.root.dir, to.root.name, replaceIfExists = true)
  }

  // for directory
  def once(to: NodePath)(fn: Patch => Unit): Unit = {
    if (to.exists)
      return
    val src = server.astral.allocDirectory(fn)
    src.root.moveTo(to.dir, to.name, replaceIfExists = false)
  }

  def replaceDirectory(to: Patch)(fn: Patch => Unit): Unit = {
    server.astral.free(to)
    val from = server.astral.allocDirectory(fn)
    from.root.moveTo(to.root.dir, to.root.name, replaceIfExists = false)
  }

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
    val src = server.astral.allocDirectory(fn)
    move(src)
  }
}
