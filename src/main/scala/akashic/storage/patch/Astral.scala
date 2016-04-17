package akashic.storage.patch

import java.nio.file.{FileAlreadyExistsException, NoSuchFileException}

import akashic.storage.backend.NodePath
import akashic.storage.strings

/** Astral is where everything is given birth and die */
case class Astral(root: NodePath) {
  def allocData[V, A](makeTemp: NodePath => Data[V], fn: Data[V] => A): (Data[V], A) = {
    val newPath = root.resolve(strings.random(32))
    val data = makeTemp(newPath)
    val res = try {
      fn(data)
    } catch {
      case e: Throwable =>
        newPath.removeIfExists
        throw e
    }
    (data, res)
  }

  def allocDirectory[A](fn: Patch => A): (Patch, A) = {
    val newPath = root.resolve(strings.random(32))
    newPath.makeDir

    val patch = Patch(newPath)
    val res = try {
      fn(Patch(newPath))
    } catch {
      case e: Throwable =>
        newPath.purgeDir
        throw e
    }
    (patch, res)
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
