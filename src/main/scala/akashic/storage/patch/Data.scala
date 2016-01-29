package akashic.storage.patch

import java.io.InputStream
import java.nio.file.Path

import akashic.storage.files

case class Data(root: Path) extends Patch {
  // FIXME rename to filePath
  val filePath: Path = root.resolve("file")
  def length: Long = files.fileSize(filePath)
  def write(inp: InputStream) = ???
  def read: Path = ???
  def writeBytes(bytes: Array[Byte]) = {
    files.writeBytes(filePath, bytes)
  }
  def readBytes: Array[Byte] = {
    files.readBytes(filePath)
  }
  def merge(files: Seq[Data]) = ???
}
