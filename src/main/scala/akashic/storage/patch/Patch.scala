package akashic.storage.patch

import java.nio.file.{Files, Path}

import akashic.storage.files

object Patch {
  def apply(_root: Path) = new Patch { def root = _root }
}

trait Patch {
  def root: Path

  def name: String = files.basename(root)

  def asData = Data(root)
  def asBucket = Bucket(root)
  def asKey = Key(root)
  def asVersion = Version(root)
  def asUpload = Upload(root)
}
