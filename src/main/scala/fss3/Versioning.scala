package fss3

import java.nio.file.{Files, Path}

import org.apache.commons.io.IOUtils

import scala.pickling.Defaults._
import scala.pickling.binary._

object Versioning {

  case class t(value: Int) {
    def write(path: Path): Unit = {
      LoggedFile(path).put { f =>
        f.writeBytes(this.pickle.value)
      }
    }
  }
  def read(path: Path): this.t = {
    using(Files.newInputStream(LoggedFile(path).get.get)) { f =>
      BinaryPickle(IOUtils.toByteArray(f)).unpickle[t]
    }
  }

  val UNVERSIONED = 0
  val ENABLED = 1
}
