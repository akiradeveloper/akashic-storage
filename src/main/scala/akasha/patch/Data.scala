package akasha.patch

import java.io.InputStream
import java.nio.file.Path

case class Data(root: Path) extends Patch {
  val data: Path = root.resolve("data")
  def write(inp: InputStream) = ???
  def read: Path = ???
  def writeBytes(data: Array[Byte]) = ???
  def readBytes: Array[Byte]  = ???
  def merge(files: Seq[Data]) = ???
  override def init {}
}
