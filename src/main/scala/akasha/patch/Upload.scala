package akasha.tree

case class Upload(root: Path) extends Patch {
  val parts = root.resolve("parts")
  def partPath(n: Int): Path = parts.resolve(n)
  val part(n: Int) = PatchLog(partPath(n))
  val acl = PatchLog(root.resolve("acl"))
  val meta = PatchLog(root.resolve("meta"))
  def init {
    Files.createDirectory(root.resolve("parts"))
    acl.init
    meta.init
  }
}
