package akasha.service

import scala.collection.mutable
import scala.pickling.Defaults._
import scala.pickling.binary._

object KVList {
  case class t(unwrap: Seq[(String, String)])
  def builder: Builder = Builder()
  case class Builder() {
    val l = mutable.ListBuffer[(String, String)]()
    def append(k: String, v: String): this.type = { 
      l += k -> v
      this
    }
    def appendOpt(k: String, v: Option[String]): this.type = {
      if (v.isDefined) { l += k -> v.get }
      this
    }
    def build = t(l)
  }
}

object Meta {
  case class t(isVersioned: Boolean,
               isDeleteMarker: Boolean,
               eTag: String,
               attrs: KVList.t,
               xattrs: KVList.t) {
    def toBytes: Array[Byte] = this.pickle.value
  }
  def fromBytes(bytes: Array[Byte]): t = {
    BinaryPickle(bytes).unpickle[t]
  }
}
