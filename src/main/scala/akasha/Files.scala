package akasha

import java.nio.file.{Files => JFiles, Path}
import org.apache.commons.io.{FileUtils, IOUtils}

object Files {

  object Implicits {
    def using[A <: AutoCloseable, B](resource: A)(f: A => B): B = {
      try {
        f(resource)
      } finally {
        resource.close
      }
    }
  }

  import Implicits._

  def writeBytes(path: Path, data: Array[Byte]) = {
    FileUtils.writeByteArrayToFile(path.toFile, data)
  }

  def readBytes(path: Path): Array[Byte] = {
    FileUtils.readFileToByteArray(path.toFile)
  }

  // def write(path: Path, inp: InputStream) = {
  //   using(JFiles.newOutputStream(path, StandardOpenOption.CREATE_NEW)) { oup =>
  //     IOUtils.copyLarge(inp, oup)
  //   }
  // }

  def touch(path: Path) = {
    FileUtils.touch(path.toFile)
  }

  import org.apache.commons.codec.digest.DigestUtils
  def computeMD5(path: Path): String = {
    using(JFiles.newInputStream(path)) { inp =>
      DigestUtils.md5Hex(inp)
    }
  }

  import java.util.Date
  def lastDate(path: Path): Date = {
    new Date(JFiles.getLastModifiedTime(path).toMillis)
  }

  def basename(path: Path): String = {
    path.getFileName.toString
  }

  def fileSize(path: Path): Long = {
    JFiles.size(path)
  }

  import org.apache.tika.Tika
  def detectContentType(path: Path): String = {
    using(JFiles.newInputStream(path)) { f =>
      val tika = new Tika()
      tika.detect(f)
    }
  }

  def children(path: Path): Seq[Path] = {
    import scala.collection.JavaConversions._
    using(JFiles.newDirectoryStream(path)) { p =>
      p.iterator.toList
    }
  }

  def purgeDirectory(path: Path) = {
    FileUtils.cleanDirectory(path.toFile)
    JFiles.delete(path)
  }
}
