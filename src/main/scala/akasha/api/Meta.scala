package akasha.api

import java.nio.file.{Files, Path, Paths}

import org.apache.commons.io.IOUtils

import scala.pickling.Defaults._
import scala.pickling.binary._
import scala.collection.mutable

object KVList {
  case class t(unwrap: Seq[(String, String)])
  def builder: Builder = Builder()
  case class Builder() {
    val l = mutable.ListBuffer[(String, String)]()
    def append(k: String, v: Option[String]) = { if (v.isDefined) { l += k -> v.get }; this }
    def build = t(l)
  }
}

object Meta {

  case class t(isVersioned: Boolean,
               isDeleteMarker: Boolean,
               eTag: String,
               attrs: KVList.t,
               xattrs: KVList.t) {
    def write(path: Path): Unit = {
      path.writeBytes(this.pickle.value)
    }
  }

  def read(path: Path): t = {
    using(Files.newInputStream(path)) { f =>
      BinaryPickle(IOUtils.toByteArray(f)).unpickle[t]
    }
  }
}

object MetaDump extends App {
  val path = Paths.get(args(0))
  val meta = Meta.read(path)
  println(meta)
}
