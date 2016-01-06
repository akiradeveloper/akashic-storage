package akasha

import java.io.{IOException, InputStream}
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{Files => JFiles, FileVisitResult, SimpleFileVisitor, StandardOpenOption, Path}
import java.util.Date

import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.{FileUtils, IOUtils}
import org.apache.tika.Tika

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
    // using(JFiles.newOutputStream(path, StandardOpenOption.CREATE_NEW)) { oup =>
    //   IOUtils.write(data, oup)
    // }
  }

  def readBytes(path: Path): Array[Byte] = {
    FileUtils.readFileToByteArray(path.toFile)
  }

  def write(path: Path, inp: InputStream) = {
    using(JFiles.newOutputStream(path, StandardOpenOption.CREATE_NEW)) { oup =>
      IOUtils.copyLarge(inp, oup)
    }
  }

  def touch(path: Path) = {
    FileUtils.touch(path.toFile)
  }

  def computeMD5(path: Path): String = {
    using(JFiles.newInputStream(path)) { inp =>
      DigestUtils.md5Hex(inp)
    }
  }

  def lastDate(path: Path): Date = new Date(JFiles.getLastModifiedTime(path).toMillis)

  def basename(path: Path): String = path.getFileName.toString

  def fileSize(path: Path): Long = JFiles.size(path)

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

  def purgeDirectory(path: Path) {
    // clean the contents
    JFiles.walkFileTree(path, new SimpleFileVisitor[Path] {
      override def visitFile(x: Path, attrs: BasicFileAttributes) = {
        JFiles.delete(x)
        FileVisitResult.CONTINUE
      }
      override def postVisitDirectory(x: Path, e: IOException) = {
        if (x == path) {
          FileVisitResult.TERMINATE
        } else {
          JFiles.delete(x)
          FileVisitResult.CONTINUE
        }
      }
    })
    // and delete itself
    JFiles.delete(path)
  }
}
