package akashic.storage.compactor

import java.nio.file.{Files, Path, FileAlreadyExistsException}
import akashic.storage.{files, strings}

case class GarbageCan(root: Path) {
  def add(path: Path) {
    val newPath = root.resolve(strings.random(32))
    try {
      // no need to be atomic because if the trash remains in the tree
      // next compaction has a chance to find it out.
      Files.move(path, newPath)
    } catch {
      case e: FileAlreadyExistsException => add(path)
      case e: Throwable => throw e
    }
    files.purgeDirectory(newPath)
  }
}
