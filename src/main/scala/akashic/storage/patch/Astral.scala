package akashic.storage.patch

import java.nio.file.{FileAlreadyExistsException, Files, NoSuchFileException, Path}
import akashic.storage.backend.NodePath
import akashic.storage.{fs, strings}

/** Astral is where everything is given birth and die */
case class Astral(root: NodePath) {
  def allocData[V](makeTemp: NodePath => Data[V], fn: Data[V] => Unit): Data[V] = {
    val newPath = root.resolve(strings.random(32))
    val data = makeTemp(newPath)
    try {
      fn(data)
    } catch {
      case e: Throwable =>
        newPath.removeIfExists
        throw e
    }
    data
  }

  def allocDirectory(fn: Patch => Unit): Patch = {
    val newPath = root.resolve(strings.random(32))
    newPath.makeDir

    val patch = Patch(newPath)
    try {
      fn(Patch(newPath))
    } catch {
      case e: Throwable =>
        newPath.purgeDir
        throw e
    }
    patch
  }

  private def moveBack(path: NodePath): Option[NodePath] = {
    val newPath = root.resolve(strings.random(32))
    try {
      // no need to be atomic because if the trash remains in the tree
      // next compaction has a chance to find it out.
      path.moveTo(newPath.dir, newPath.name, replaceIfExists = false)
    } catch {
      case e: NoSuchFileException => return None
      case e: FileAlreadyExistsException => moveBack(path)
      case e: Throwable => throw e
    }
    Some(newPath)
  }

  def free[V](data: Data[V]): Unit = {
    moveBack(data.root) match {
      case Some(a) => a.purgeDir
      case None =>
    }
  }

  def free(dir: NodePath): Unit = {
    if (!dir.exists)
      return
    moveBack(dir) match {
      case Some(a) => a.purgeDir
      case None =>
    }
  }

  def free(dir: Patch): Unit = {
    free(dir.root)
  }
}
