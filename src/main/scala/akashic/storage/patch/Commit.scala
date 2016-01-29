package akashic.storage.patch

import java.nio.file._

import akashic.storage.server

object Commit {
  private def preparePatch(fn: Patch => Unit): Patch = {
    val patch = Patch(server.astral.alloc)
    try {
      fn(patch)
    } catch {
      case e: Throwable =>
        server.astral.free(patch.root)
        throw e
    }
    patch
  }

  def once(to: Path)(fn: Patch => Unit) = Once(to)(fn).run
  private case class Once(to: Path)(fn: Patch => Unit) {
    def run {
      if (Files.exists(to))
        return
      val src = preparePatch(fn)
      Files.move(src.root, to)
    }
  }

  def replaceData(to: Data)(fn: Patch => Unit) = replace(to.root)(fn)
  def replace(to: Path)(fn: Patch => Unit) = Replace(to)(fn).run
  private case class Replace(to: Path)(fn: Patch => Unit) {
    def run: Unit = {
      val src = preparePatch(fn)
      server.astral.free(to)
      Files.move(src.root, to)
    }
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
      val src = preparePatch(fn)
      move(src)
    }
  }
}
