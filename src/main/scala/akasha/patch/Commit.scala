package akasha.patch

object Commit {
  case class Once(to: Path, fn: Patch => Unit) {
    def run: Boolean = {
      Try {
        val patch = Patch(to)
        if (Files.exists(to) && !pseudoPatch.committed) {
          akasha.Files.purgeDirectory(to)
        }
        patch.init
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
  case class RetryGeneric(makePath: () => Path, f: Patch => Unit) {
    def run: Patch = {
      try {
        val patch = Patch(makePath)
        patch.init
        fn(patch)
        patch.commit
        patch
      } catch {
        case FileAlreadyExistsException(_) => run
        case e: Throwable => throw e
      }
    }
  }
  case class Retry(to: PatchLog, f: Patch => Unit) {
    def run: Patch = RetryGeneric(() => to.acquireNewLoc, f).run
  }
}
