package akashic.storage.patch

import java.nio.file.{Files, Path}

import akashic.storage.files

case class Data(root: Path) extends Patch {
  val filePath: Path = root.resolve("file")
  def length: Long = files.fileSize(filePath)
  def write(bytes: Array[Byte]) = {
    files.writeBytes(filePath, bytes)
  }
  def read: Array[Byte] = {
    files.readBytes(filePath)
  }
  def readOpt: Option[Array[Byte]] = {
    if (Files.exists(root)) {
      Some(read)
    } else {
      None
    }
  }
}
