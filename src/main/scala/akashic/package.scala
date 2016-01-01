import java.io.IOException
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file._
import java.text.SimpleDateFormat
import java.util.{TimeZone, Date}

import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.IOUtils
import org.apache.tika.Tika

package object akashic {

  def using[A <: AutoCloseable, B](resource: A)(f: A => B): B = {
    try {
      f(resource)
    } finally {
      resource.close
    }
  }

  implicit class PathOps(path: Path) {

    def writeBytes(data: Array[Byte]) {
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

  implicit class StringOps(self: String) {
    def compIns(other: String): Boolean = {
      self.toLowerCase == other.toLowerCase
    }
    def optInt: Option[Int] = {
      try {
        Some(self.toInt)
      } catch {
        case _: Throwable =>  None
      }
    }
  }

  implicit class DateOps(self: Date) {
    // 'Z' means UTC
    def format000Z = {
      val sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
      sdf.setTimeZone(TimeZone.getTimeZone("UTC"))
      sdf.format(self)
    }
  }

  implicit class BoolOps(b: => Boolean) {
    def orFailWith(e: Error.t): Unit = {
      if (!b) {
        Error.failWith(e)
      }
    }
    def not: Boolean = {
      !b
    }
  }

  implicit class AnyOps[A](a: A) {
    def `|>`[B](f: A => B): B = f(a)

    def applyIf(p: => Boolean)(f: A => A): A = {
      if (p) {
        f(a)
      } else {
        a
      }
    }
    // FIXME not sure this is correct (seems so dangerous)
    def applySome[B, C](x: Option[B])(f: A => B => C): C = {
      x match {
        case Some(b) => f(a)(b)
        case None => a.asInstanceOf[C]
      }
    }
  }

  implicit class OptionOps[A](a: Option[A]) {
    def noneOrSome[B](default: B)(f: A => B): B = {
      if (a.isDefined) {
        f(a.get)
      } else {
        default
      }
    }
    def `<+`(b: Option[A]): Option[A] = {
      (a, b) match {
        case (Some(_), _) => a
        case _ => b
      }
    }
  }
}
