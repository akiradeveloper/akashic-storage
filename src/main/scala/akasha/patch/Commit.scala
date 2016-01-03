package akasha.patch

object Commit {
  case class Once(patch: Patch, to: Path) {
    def run: Boolean = {
      Try {
        val pseudoPatch = new Patch {
          def root = to
        }
        if (Files.exists(to) && !pseudoPatch.commited) {
          akasha.Files.purgeDirectory(to)
        }
        Files.move(patch.root, to, StandardCopyOption.ATOMIC_MOVE)
        patch.commit
      } match {
        Success(a) => true
        Failure(a) => false
      }
    }
  }
  // upload part can overwrite the existing part
  case class ForceOnce(patch: Patch, to: Path) {
    def run {
      Files.move(patch.root, to, StandardCopyOption.ATOMIC_MOVE | StandardCopyOption.REPLACE_EXISTING)
      patch.commit
    }
  }
  case class Retry(patch: Patch, to: PatchLog) {
    def run: Path = {
      try {
        val newPath = to.acquireNewLoc
        Files.move(patch.root, to.acquireNewLoc, StandardCopyOption.ATOMIC_MOVE)
        patch.commit
        newPath
      } catch {
        case FileAlreadyExistsException(_) => retry
        case e -> throw e
      }
    }
  }
}
