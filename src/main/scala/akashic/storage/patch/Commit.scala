package akashic.storage.patch

import java.nio.file._

import akashic.storage.caching.Cache
import akashic.storage.server

object Commit {
  // for file
  def replaceData[V](to: Data[V], makeTemp: Path => Data[V])(fn: Data[V] => Unit): Unit = {
    val from = server.astral.allocData(makeTemp, fn)
    Files.move(from.root, to.root, StandardCopyOption.REPLACE_EXISTING)
  }

  // for directory
  def once(to: Path)(fn: Patch => Unit): Unit = {
    if (Files.exists(to))
      return
    val src = server.astral.allocDirectory(fn)
    Files.move(src.root, to)
  }

  def replaceDirectory(to: Patch)(fn: Patch => Unit): Unit = {
    server.astral.free(to)
    val from = server.astral.allocDirectory(fn)
    Files.move(from.root, to.root)
  }

  def retry(alloc: () => Path)(fn: Patch => Unit): Patch = {
    def move(src: Patch): Patch = {
      val dest = Patch(alloc())
      try {
        Files.move(src.root, dest.root)
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
