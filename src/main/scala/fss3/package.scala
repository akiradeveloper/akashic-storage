import java.io.IOException
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file._
import java.util.Date

import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.IOUtils
import org.apache.tika.Tika

package object fss3 {

  def using[A <: AutoCloseable, B](resource: A)(f: A => B): B = {
    try {
      f(resource)
    } finally {
      resource.close
    }
  }

  implicit class PathOps(path: Path) {

    def writeBytes(data: Array[Byte]): Unit = {
      using(Files.newOutputStream(path, StandardOpenOption.CREATE)) { f =>
        f.write(data)
        f.flush
      }
    }

    def readBytes: Array[Byte] = {
      using(Files.newInputStream(path)) { f =>
        IOUtils.toByteArray(f)
      }
    }

    def computeMD5: String = {
      using(Files.newInputStream(path)) { inp =>
        DigestUtils.md5Hex(inp)
      }
      //Base64.encodeBase64String(DigestUtils.md5(Files.newInputStream(path)))
    }

    def length: Long = {
      Files.size(path)
    }

    def touch = Files.createFile(path)

    def delete = {
      if (exists) {
        Files.delete(path)
      }
    }

    def exists = Files.exists(path)

    def mkdirp {
      if (!exists) {
        Files.createDirectories(path)
      }
    }

    def children: Seq[Path] = {
      import scala.collection.JavaConversions._
      using(Files.newDirectoryStream(path)) { p =>
        p.iterator.toList
      }
    }

    def lastName: String = path.getFileName.toString

    def contentType: String = {
      // Files.probeContentType(path)
      using(Files.newInputStream(path)) { f =>
        val tika = new Tika()
        tika.detect(f)
      }
    }

    def emptyDirectory = Files.walkFileTree(path, new SimpleFileVisitor[Path] {
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

    def lastModified: Date = {
      new Date(Files.getLastModifiedTime(path).toMillis)
    }
  }
}
