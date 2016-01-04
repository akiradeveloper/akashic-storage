package akasha.patch

import java.nio.file.{Files, Path}

case class Upload(root: Path) extends Patch {
  val parts = root.resolve("parts")
  def partPath(n: Int): Path = parts.resolve(n.toString)
  def part(n: Int) = PatchLog(partPath(n))
  val acl = PatchLog(root.resolve("acl"))
  val meta = PatchLog(root.resolve("meta"))
  override def init {
    Files.createDirectory(root.resolve("parts"))
    acl.init
    meta.init
  }
}
