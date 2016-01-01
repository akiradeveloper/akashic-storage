package akashic.tree

case class Version(root: Path) {
  val data = DataPatch(path.resolve("data"))
  val acl = PatchLog(path.resolve("acl"))
  val meta = PatchLog(path.resolve("meta"))
}
