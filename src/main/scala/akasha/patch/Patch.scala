package akasha.patch

trait Patch {
  def root: Path
  def commitPath = root.resolve("commit")

  // We assume file creation is atomic
  def commit {
    akasha.Files.touch(commitPath)
  }
  def commited: Boolean = {
    Files.exists(commitPath)
  }
}
