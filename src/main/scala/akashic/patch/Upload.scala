package akashic.tree

case class UploadPatch(root: Path) {
  val part(id: Int) = root.resolve("parts").resolve(id)
  val acl = PatchLog(root.resolve("acl"))
  val meta = PatchLog(root.resolve("meta"))

  def mergeParts: DataPatch = ???
}
