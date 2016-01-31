package akashic.storage.patch

import java.nio.file._

import akashic.storage.server

object Commit {
  // for file
  def replaceData(to: Data)(fn: Data => Unit) = {
    val from = server.astral.allocData(fn)
    Files.move(from.root, to.root, StandardCopyOption.REPLACE_EXISTING)
  }

  // for directory
  def once(to: Path)(fn: Patch => Unit) = Once(to)(fn).run
  private case class Once(to: Path)(fn: Patch => Unit) {
    def run {
      if (Files.exists(to))
        return
      val src = server.astral.allocDirectory(fn)
      Files.move(src.root, to)
    }
  }

  def replaceDirectory(to: Patch)(fn: Patch => Unit) = {
    server.astral.free(to)
    val from = server.astral.allocDirectory(fn)
    Files.move(from.root, to.root)
  }

  def retry(alloc: () => Path)(fn: Patch => Unit) = Retry(alloc)(fn).run
  private case class Retry(alloc: () => Path)(fn: Patch => Unit) {
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
    def run: Patch = {
      val src = server.astral.allocDirectory(fn)
      move(src)
    }
  }
}
