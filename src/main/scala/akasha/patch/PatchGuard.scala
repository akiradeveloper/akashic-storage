package akasha.patch

case class PatchGuard(root: Path) extends Patch {
  // throws if the dir exists
  def init {
    Files.createDirectory(root)
  }
}
