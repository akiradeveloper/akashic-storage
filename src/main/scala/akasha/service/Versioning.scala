package akasha.service

import scala.pickling.Defaults._
import scala.pickling.binary._

object Versioning {
  case class t(value: Int) {
    def toBytes: Array[Byte] = this.pickle.value
  }
  def read(bytes: Array[Byte]): t = BinaryPickle(bytes).unpickle[t]

  val UNVERSIONED = 0
  val ENABLED = 1
}
