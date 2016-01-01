package akashic.patch

case class PatchBox(root: Path) {
  def acquirePatchLoc: Path = {
    Files.createDirectory(newPath)
  }
}
