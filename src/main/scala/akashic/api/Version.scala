package akashic.api

case class Version(parent: Key, root: Path) {
  val data = DataPatch(path.resolve("data"))
  val acl = PatchLog(path.resolve("acl"))
  val meta = PatchLog(path.resolve("meta"))

  def id: Int = ???
}
