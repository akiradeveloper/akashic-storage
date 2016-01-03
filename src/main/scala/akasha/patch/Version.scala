package akasha.patch

case class Version(root: Path) extends Patch {
  val data = Data(path.resolve("data"))
  val acl = PatchLog(path.resolve("acl"))
  val meta = PatchLog(path.resolve("meta"))
  def init {
    data.init
    acl.init
    meta.init
  }
}
