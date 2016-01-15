package akashic.storage.patch

import java.nio.file.{Files, Path}

case class Version(root: Path) extends Patch {
  def key = Key(root.getParent.getParent)
  val data = Data(root.resolve("data"))
  val meta = Data(root.resolve("meta"))
  val acl = PatchLog(root.resolve("acl"))
  override def init {
    Files.createDirectory(data.root)
    data.init
    Files.createDirectory(meta.root)
    meta.init
    Files.createDirectory(acl.root)
  }
}
