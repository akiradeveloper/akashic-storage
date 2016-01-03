package akasha.patch

case class Key(root: Path) extends Patch {
  val versions = PatchLog(path.resolve("versions"))
  def init {
    versions.init
  }
  def findVersion(id: Int): Option[Version] = ???
}
