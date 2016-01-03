package akasha.patch

object Commit {
  case class Once(to: Path, fn: Patch => Unit) {
    def run: Boolean = {
      Try {
        val patch = PatchGuard(to)
        if (Files.exists(to) && !pseudoPatch.commited) {
          akasha.Files.purgeDirectory(to)
        }
        patch.init
        fn(patch)
        patch.commit
      } match {
        Success(a) => true
        Failure(a) => false
      }
    }
  }

  case class RetryGeneric(makePath: => Path, f: Patch => Unit) {
    def run: Patch = {
      try {
        val patch = PatchGuard(makePath)
        patch.init
        fn(patch)
        patch.commit
        patch
      } catch {
        case FileAlreadyExistsException(_) => run
        case e -> throw e
      }
    }
  }

  case class Retry(to: PatchLog, f: Patch => Unit) {
    def run: Patch = RetryGeneric(() => to.acquireNewLoc, f).run
  }
}
