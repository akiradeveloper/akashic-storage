package akashic.storage.patch

import java.nio.file.{FileAlreadyExistsException, Files, NoSuchFileException, Path}

import akashic.storage.{files, strings}

// Astral is where everything is given birth and die
case class Astral(root: Path) {
  def allocData(fn: Data => Unit): Data = {
    val newPath = root.resolve(strings.random(32))
    val data = Data(newPath)
    try {
      fn(data)
    } catch {
      case e: Throwable =>
        Files.deleteIfExists(newPath)
        throw e
    }
    data
  }

  def allocDirectory[T <: Patch](fn: Patch => Unit): Patch = {
    val newPath = root.resolve(strings.random(32))
    Files.createDirectory(newPath)
    val patch = Patch(newPath)
    try {
      fn(Patch(newPath))
    } catch {
      case e: Throwable =>
        files.purgeDirectory(newPath)
        throw e
    }
    patch
  }

  private def moveBack(path: Path): Option[Path] = {
    val newPath = root.resolve(strings.random(32))
    try {
      // no need to be atomic because if the trash remains in the tree
      // next compaction has a chance to find it out.
      Files.move(path, newPath)
    } catch {
      case e: NoSuchFileException => return None
      case e: FileAlreadyExistsException => moveBack(path)
      case e: Throwable => throw e
    }
    Some(newPath)
  }

  def free(data: Data): Unit = {
    Files.delete(moveBack(data.root).get)
  }

  def free(dir: Path): Unit = {
    moveBack(dir) match {
      case Some(a) => files.purgeDirectory(a)
      case None =>
    }
  }

  def free(dir: Patch): Unit = {
    free(dir.root)
  }
}
