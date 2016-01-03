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
  def get: Option[Patch] = {
    maxId match {
      case 0 => None
      case a => Some(Patch(root.resolve(maxId)))
    }
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
