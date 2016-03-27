package akashic.storage.patch

import java.nio.file.{Files, Path}

import akashic.storage.backend.NodePath
import org.apache.commons.io.FileUtils

trait Data[V] extends Patch {
  val filePath: NodePath
  def length: Long = filePath.getAttr.length
  def root = filePath
  def get: V
  def put(v: V)
}

object Data {
  case class Pure(filePath: NodePath) extends Data[Array[Byte]] {
    override def get: Array[Byte] = root.readFile
    override def put(v: Array[Byte]): Unit = root.createFile(v)
  }
  object Pure {
    def make(path: NodePath) = Pure(path)
  }
}
