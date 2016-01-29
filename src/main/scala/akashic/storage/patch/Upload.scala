package akashic.storage.patch

import java.nio.file.{Files, Path}

import akashic.storage.files

case class Upload(root: Path) extends Patch {
  val parts = root.resolve("parts")
  private def partPath(n: Int): Path = parts.resolve(n.toString)
  def part(n: Int) = Part(partPath(n))
  val meta = Data(root.resolve("meta"))
  val acl = Data(root.resolve("acl"))
  def init {
    Files.createDirectory(parts)
  }
  def findPart(partNumber: Int): Option[Part] = {
    val path = partPath(partNumber)
    if (Files.exists(path)) {
      Some(Part(path))
    } else {
      None
    }
  }
  def listParts: Seq[Part] = files.children(parts).map(Part(_)).sortBy(_.id)
}
