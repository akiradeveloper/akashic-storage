package akashic.storage.patch

import akashic.storage.backend.NodePath
import akashic.storage.caching.Cache

trait Data[V] extends Patch {
  val filePath: NodePath
  def root = filePath
  def get: V
  def replace(v: V, creTime: Long)
  def put(v: V) = replace(v, 0)
}

object Data {
  case class Pure(filePath: NodePath) extends Data[Array[Byte]] {
    override def get: Array[Byte] = root.readFile
    override def replace(v: Array[Byte], creTime: Long): Unit = root.createFile(v)
  }
  object Pure {
    def make(path: NodePath) = Pure(path)
  }
}
