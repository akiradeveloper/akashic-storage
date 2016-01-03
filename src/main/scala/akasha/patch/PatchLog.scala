package akasha.patch

case class PatchLog(root: Path) extends Patch {
  def acquireNewLoc: Path = {
    val xs = root.children.map(_.toInt)
    val newId = if (xs.isEmpty) {
      1
    } else {
      xs.max + 1
    }
    root.resolve(id)
  }
  // return by descending order
  def listVersions: Seq[Patch] = {
    Files.children(root).map(Patch(_)).filter(_.committed).sortBy(-1 * _.name.toInt)
  }
  // returns the newest version within committed
  def get: Option[Patch] = {
    val ls = listVersions
    ls.headOption
  }
  def get(id: Int): Option[Patch] = {
    if (!Files.exists(root.resolve(id))) {
      return None
    }
    if (!Patch(root.resolve(id)).committed) {
      return None
    }
    Patch(root.resolve(id))
  }
}
