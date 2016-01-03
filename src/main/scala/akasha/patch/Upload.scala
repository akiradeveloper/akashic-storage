package akasha.tree

case class Upload(root: Path) extends Patch {
  val parts = root.resolve("parts")
  val part(id: Int): Data = Data(parts.resolve(id))
  val acl = PatchLog(root.resolve("acl"))
  val meta = PatchLog(root.resolve("meta"))
  def init {
    Files.createDirectory(root.resolve("parts"))
    acl.init
    meta.init
  }
}
