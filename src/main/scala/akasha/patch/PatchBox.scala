package akasha.patch

case class PatchBox(root: Path) {

  def retry: Path = {
    try {
      val newPath = akasha.Strings.random(16)
      Files.createDirectory(newPath)
    } catch {
      case FileAlreadyExistsException(_) => retry
      case e -> throw e
    }
  }

  def acquirePatchLoc: Path = {
    retry
  }
}
