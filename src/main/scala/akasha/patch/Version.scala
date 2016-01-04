package akasha.patch

import java.nio.file.Path

case class Version(root: Path) extends Patch {
  val data = Data(root.resolve("data"))
  val acl = PatchLog(root.resolve("acl"))
  val meta = PatchLog(root.resolve("meta"))
  override def init {
    data.init
    acl.init
    meta.init
  }
}
