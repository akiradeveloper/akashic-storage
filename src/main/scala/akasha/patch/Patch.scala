package akasha.patch

object Patch {
  def apply(root: Path) = new Patch { def root = root }
}

trait Patch {
  def root: Path
  def commitPath = root.resolve("commit")

  // We assume file creation is atomic
  def commit {
    akasha.Files.touch(commitPath)
  }
  def committed: Boolean = {
    Files.exists(commitPath)
  }
  // throws if the dir exists
  def init {
    Files.createDirectory(root)
  }
}
