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
  private def listPatches: Seq[Patch] = {
    files.children(root).map(Patch(_)).sortBy(-1 * _.name.toInt)
  }
  def find: Option[Patch] = {
    val ls = listPatches
    ls.headOption
  }
}
