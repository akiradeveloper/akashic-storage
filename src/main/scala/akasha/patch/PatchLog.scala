package akasha.patch

case class PatchLog(root: Path) {
  def init {
    Files.createDirectory(root)
  }
  def acquireNewLoc: Path = {
    val xs = root.children.map(_.toInt)
    val newId = if (xs.isEmpty) {
      1
    } else {
      xs.max + 1
    }
    root.resolve(id)
  }
  def get: Option[Int] = {
    maxId match {
      case 0 => None
      case a => Some(a)
    }
  }
  def path(id: Int): Path = root.resolve(id)
}
