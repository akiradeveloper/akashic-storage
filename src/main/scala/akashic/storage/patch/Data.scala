package akashic.storage.patch

import java.nio.file.{Files, Path}

import akashic.storage.files
import org.apache.commons.io.FileUtils

trait Data[V] extends Patch {
  val filePath: Path
  def length: Long = files.fileSize(filePath)
  def root = filePath
  def get: V
  def put(v: V)
}

object Data {
  case class Pure(filePath: Path) extends Data[Array[Byte]] {
    override def get: Array[Byte] = files.readBytes(filePath)
    override def put(v: Array[Byte]): Unit = files.writeBytes(filePath, v)
  }
  object Pure {
    def make(path: Path) = Pure(path)
  }
}
