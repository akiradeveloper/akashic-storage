package akasha.patch

import java.nio.file.{FileAlreadyExistsException, Files, Path}

import akasha.files

import scala.util.{Failure, Success, Try}

object Commit {
  def once(to: Path)(fn: Patch => Unit) = Once(to)(fn).run
  private case class Once(to: Path)(fn: Patch => Unit) {
    def run: Boolean = {
      Try {
        val patch = Patch(to)
        if (Files.exists(to) && !patch.committed) {
          files.purgeDirectory(to)
        }
        Files.createDirectory(patch.root)
        // As the previous line throws exception if the directory exists
        // only a process created the directory can reach this line.
        fn(patch)
        patch.commit
      } match {
        case Success(a) => true
        case Failure(a) => false
      }
    }
  }
  // used by multipart upload just to allocate empty directory
  case class RetryGenericNoCommit(makePath: () => Path)(fn: Patch => Unit) {
    def run: Patch = {
      try {
        val patch = Patch(makePath())
        Files.createDirectory(patch.root)
        fn(patch)
        patch
      } catch {
        case e: FileAlreadyExistsException => run
        case e: Throwable => throw e
      }
    }
  }
  case class RetryGeneric(makePath: () => Path)(fn: Patch => Unit) {
    def run = RetryGenericNoCommit(makePath) { patch => fn(patch); patch.commit }.run
  }
  def retry(to: PatchLog)(fn: Patch => Unit) = Retry(to)(fn).run
  private case class Retry(to: PatchLog)(fn: Patch => Unit) {
    def run: Patch = RetryGeneric(() => to.acquireNewLoc)(fn).run
  }
}
