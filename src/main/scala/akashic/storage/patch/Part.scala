package akashic.storage.patch

import java.nio.file.{Path, Files}

case class Part(root: Path) extends Patch {
  val versions = PatchLog(root.resolve("versions"))
  override def init = {
    Files.createDirectory(versions.root)
  }
  def id = name.toInt
  def find: Option[Data] = versions.find.map(_.asData)
}
