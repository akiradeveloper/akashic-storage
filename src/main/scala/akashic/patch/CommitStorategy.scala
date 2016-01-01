package akashic.patch

object CommitStorategy {
  case class Once(patch: Patch, to: Path) {
    def run: Boolean = {
      Try {
        Files.move(patch.root, to, StandardCopyOption.ATOMIC_MOVE)
      } match {
        Success(a) => true
        Failure(a) => false
      }
    }
  }

  case class Force(patch: Patch, to: Path) {
    def run {
      Files.move(patch.root, to, StandardCopyOption.ATOMIC_MOVE | StandardCopyOption.REPLACE_EXISTING)
    }
  }

  case class Retry(patch: Patch, to: PatchLog) {
    def run: Path = {
      try {
        val newPath = to.acquireNewLoc
        Files.move(patch.root, to.acquireNewLoc, StandardCopyOption.ATOMIC_MOVE)
        newPath
      } catch {
        case _ => run
      }
    }
  }
}
