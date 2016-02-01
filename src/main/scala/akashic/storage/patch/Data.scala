package akashic.storage.patch

import java.nio.file.{Files, Path}

import akashic.storage.files
import com.twitter.concurrent.AsyncStream
import com.twitter.io.{Reader, Writer, Buf}
import com.twitter.util.Future
import org.apache.commons.io.FileUtils

case class Data(root: Path) extends Patch {
  val filePath: Path = root
  def length: Long = files.fileSize(filePath)
  def write(bytes: Array[Byte]) = {
    files.writeBytes(filePath, bytes)
  }
  def writeStream(bytes: AsyncStream[Buf]): Future[Unit] = {
    val readers = bytes.map(Reader.fromBuf(_))
    val writer = Writer.fromOutputStream(FileUtils.openOutputStream(filePath.toFile))
    Reader.copyMany(readers, writer).ensure(writer.close())
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
