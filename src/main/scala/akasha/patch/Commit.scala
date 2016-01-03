package akasha.patch

object Commit {
  type Fn = Patch => Unit

  val makePatch(to: Path) = new Patch { def root = to }

  case class Once(to: Path, fn: Fn) {
    def run: Boolean = {
      Try {
        val patch = makePatch(to)
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
  // upload part can overwrite the existing part
  case class ForceOnce(to: Path, fn: Fn) {
    def run {
      if (Files.exists(to)) {
        akasha.Files.purgeDirectory(to)
      }
      val patch = makePatch(to)
      patch.init
      fn(patch)
      patch.commit
    }
  }
  case class Retry(to: PatchLog, f: Fn) {
    def run: Path = {
      try {
        val newPath = to.acquireNewLoc
        val patch = makePatch(newPath)
        patch.init
        fn(patch)
        patch.commit
        newPath
      } catch {
        case FileAlreadyExistsException(_) => retry
        case e -> throw e
      }
    }
  }
}
