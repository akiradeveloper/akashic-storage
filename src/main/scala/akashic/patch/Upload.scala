package akashic.tree

case class Upload(root: Path) extends Patch {
  val part(id: Int): Data = Data(root.resolve("parts").resolve(id))
  val acl = PatchLog(root.resolve("acl"))
  val meta = PatchLog(root.resolve("meta"))
}
