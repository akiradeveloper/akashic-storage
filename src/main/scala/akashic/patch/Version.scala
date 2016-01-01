package akashic.tree

case class Version(root: Path) extends Patch extends Leaf {
  val data = Data(path.resolve("data"))
  val acl = PatchLog(path.resolve("acl"))
  val meta = PatchLog(path.resolve("meta"))
}
