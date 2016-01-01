package akashic.patch

case class Key(root: Path) extends Patch {
  val versions = PatchLog(path.resolve("versions"))
  def init {
    Files.createDirectory(versions)
  }
  def findVersion(id: Int): Option[Version] = ???
}
