package akashic.store

case class PatchLog(root: Path) {
  def acquireNewLoc: Path = {
    val xs = root.children.map(_.toInt)
    val newId = if (xs.isEmpty) {
      0
    } else {
      xs.max + 1
    }
    root.resolve(newId)
  }
}
