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
      case a => Some(AnyPatch(root.resolve(maxId)))
    }
  }
}
