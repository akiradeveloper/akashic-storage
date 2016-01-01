package akashic.patch

case class KeyPatch(root: Path) extends Patch {
  val versions = PatchLog(path.resolve("versions"))
  def init {
    Files.createDirectory(versions)
  }
}
