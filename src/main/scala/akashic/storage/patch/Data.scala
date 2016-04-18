package akashic.storage.patch

import akashic.storage.backend.NodePath

trait Data[V] extends Patch {
  val filePath: NodePath
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
