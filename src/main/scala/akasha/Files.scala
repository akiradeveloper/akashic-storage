package akasha

import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.IOUtils
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

  def writeBytes(path: Path, data: Array[Byte]) = ???

  def readBytes(path: Path): Array[Byte] = ???

  def write(path: Path, inp: InputStream) = {
    using(Files.newOutputStream(path, StandardOpenOption.CREATE)) { oup =>
      Files.copyLarge(inp, oup)
    }
  }

  def computeMD5(path: Path): String = {
    using(Files.newInputStream(path)) { inp =>
      DigestUtils.md5Hex(inp)
    }
  }

  def lastDate(path: Path): Date = new Date(Files.getLastModifiedTime(path).toMillis)

  def basename(path: Path): String = path.getFileName.toString

  def fileSize(path: Path): Long = Files.size(path)

  def detectContentType(path: Path): String = {
    using(Files.newInputStream(path)) { f =>
      val tika = new Tika()
      tika.detect(f)
    }
  }

  def deleteDirectory(path: Path) {
    // clean the contents
    Files.walkFileTree(path, new SimpleFileVisitor[Path] {
      override def visitFile(x: Path, attrs: BasicFileAttributes) = {
        Files.delete(x)
        FileVisitResult.CONTINUE
      }
      override def postVisitDirectory(x: Path, e: IOException) = {
        if (x == path) {
          FileVisitResult.TERMINATE
        } else {
          Files.delete(x)
          FileVisitResult.CONTINUE
        }
      }
    })
    // and delete itself
    Files.delete(path)
  }
}
