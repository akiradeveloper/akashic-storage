package akasha.patch

import java.io.InputStream
import java.nio.file.Path

import akasha.files

case class Data(root: Path) extends Patch {
  // FIXME rename to filePath
  val data: Path = root.resolve("data")
  def write(inp: InputStream) = ???
  def read: Path = ???
  def writeBytes(bytes: Array[Byte]) = {
    files.writeBytes(data, bytes)
  }
  def readBytes: Array[Byte] = {
    files.readBytes(data)
  }
  def merge(files: Seq[Data]) = ???
  def init {}
}
