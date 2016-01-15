package akashic.storage.service

import scala.pickling.Defaults._
import scala.pickling.binary._

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
