package akashic.store

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

  case class Retry(patch: Patch, to: PatchLog) {
    def run {
      try {
        Files.move(patch.root, to.acquireNewLoc, StandardCopyOption.ATOMIC_MOVE)
      } catch {
        case _ => run
      }
    }
  }
}
