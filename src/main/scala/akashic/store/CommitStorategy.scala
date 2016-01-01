package akashic.store
object CommitStorategy {
  case class Once(patch: Patch, to: Path) {
  }

  case class Retry(from: Patch, to: SerialPatch) {
    def run {
    }
  }
}
