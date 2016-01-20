package akashic.storage.patch

import java.nio.file.{FileAlreadyExistsException, Files, Path}

import akashic.storage.files

case class PatchLog(root: Path) {
  def acquireNewLoc: Path = {
    val xs = files.children(root).map(files.basename(_).toInt)
    val newId = if (xs.isEmpty) {
      1
    } else {
      xs.max + 1
    }
    root.resolve(newId.toString)
  }
  // return by descending order
  def listPatches: Seq[Patch] = {
    files.children(root).map(Patch(_)).sortBy(-1 * _.name.toInt)
  }
  // returns the newest version within committed
  def find: Option[Patch] = {
    val ls = listPatches.filter(_.committed)
    ls.headOption
  }
  def find(id: Int): Option[Patch] = {
    val path = root.resolve(id.toString)
    if (Files.exists(path) && Patch(path).committed) {
      Some(Patch(path))
    } else {
      None
    }
  }
  def get(id: Int): Patch = Patch(root.resolve(id.toString))
}
