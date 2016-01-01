package akashic.tree

case class Version(root: Path) {
  val data = path.resolve("data")
  val acl = path.resolve("acl")
  val meta = path.resolve("meta")
}
