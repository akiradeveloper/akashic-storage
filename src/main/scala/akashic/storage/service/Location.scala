package akashic.storage.service

import scala.pickling.Defaults._
import scala.pickling.binary._

object Location {
  case class t(value: String) {
    def toBytes: Array[Byte] = this.pickle.value
    def toXML = <LocationConstraint>{value}</LocationConstraint>
  }
  def fromBytes(bytes: Array[Byte]): t = BinaryPickle(bytes).unpickle[t]
}
