package akasha.patch

case class Key(root: Path) extends Patch {
  val versions = PatchLog(path.resolve("versions"))
  val uploads: Path = root.resolve("uploads")
  def init {
    versions.init
    Files.createDirectory(uploads)
  }
  def findVersion(id: Int): Option[Version] = ???
}
