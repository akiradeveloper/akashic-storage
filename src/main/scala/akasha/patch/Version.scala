package akasha.patch

import java.nio.file.Path

case class Version(root: Path) extends Patch {
  val data = Data(root.resolve("data"))
  val meta = Data(root.resolve("meta"))
  val acl = PatchLog(root.resolve("acl"))
  def init {
    data.init
    acl.init
    meta.init
  }
}
