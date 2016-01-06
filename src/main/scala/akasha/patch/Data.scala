package akasha.patch

import java.io.InputStream
import java.nio.file.Path

case class Data(root: Path) extends Patch {
  val data: Path = root.resolve("data")
  def write(inp: InputStream) = ???
  def read: Path = ???
  def writeBytes(bytes: Array[Byte]) = {
    akasha.Files.writeBytes(data, bytes)
  }
  def readBytes: Array[Byte] = {
    akasha.Files.readBytes(data)
  }
  def merge(files: Seq[Data]) = ???
  def init {}
}
