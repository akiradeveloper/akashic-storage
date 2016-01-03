package akasha.patch

case class Key(root: Path) extends Patch {
  val versions = PatchLog(path.resolve("versions"))
  val uploads = Uploads(root.resolve("uploads"))
  def init {
    versions.init
    uploads.init
  }
  def findLatestVersion: Option[Version] = {
    versions.get.map(Version(_.root))
  }
  def findVersion(id: Int): Option[Version] = {
    versions.get(id).map(Version(_.root))
  }
}
