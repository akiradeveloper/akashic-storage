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
  }

  // TODO run in background and periodically
  // (no need to purge trashes on shutdown)
  def cleanup {
    // FIXME may try to delete non-existent directory
    // in case other node has deleted it.
    files.children(root).foreach { dir =>
      files.purgeDirectory(dir)
    }
  }
}
