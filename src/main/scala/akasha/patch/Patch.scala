package akasha.patch

import java.nio.file.{Files, Path}

object Patch {
  def apply(_root: Path) = new Patch { def root = _root }
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

  def name: String = {
    akasha.Files.basename(root)
  }

  def asData = Data(root)
  def asVersion = Version(root)
  def asUpload = Upload(root)
  def asBucket = Bucket(root)
  def asKey = Key(root)
}
