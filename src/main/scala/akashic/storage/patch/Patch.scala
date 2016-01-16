package akashic.storage.patch

import java.nio.file.{Files, Path}

import akashic.storage.files

object Patch {
  def apply(_root: Path) = new Patch { def root = _root }
}

trait Patch {
  def root: Path
  def init {}

  def commitPath = root.resolve("commit")

  // We assume file creation is atomic
  def commit {
    files.touch(commitPath)
  }

  def committed: Boolean = {
    Files.exists(commitPath)
  }

  def name: String = {
    files.basename(root)
  }

  def asData = Data(root)
  def asVersion = Version(root)
  def asUpload = Upload(root)
  def asBucket = Bucket(root)
  def asKey = Key(root)
  def asPart = Part(root)
}
