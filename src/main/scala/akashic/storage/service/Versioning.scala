package akashic.storage.service

import java.nio.file.Path

import akashic.storage.caching.{CacheMap, Cache}

import scala.pickling.Defaults._
import scala.pickling.binary._

object Versioning {
  def makeCache(path: Path) = new Cache[Versioning.t] {
    override def cacheMap: CacheMap[K, Versioning.t] = new CacheMap.Null[K, Versioning.t]()
    override def writer: (Versioning.t) => Array[Byte] = Versioning.writer
    override def reader: (Array[Byte]) => Versioning.t = Versioning.reader
    override val filePath: Path = path
  }
  def writer(a: t) = a.toBytes
  def reader(a: Array[Byte]): t = fromBytes(a)
  case class t(value: Int) {
    def toBytes: Array[Byte] = this.pickle.value
    def enabled = value == ENABLED
  }
  def fromBytes(bytes: Array[Byte]): t = BinaryPickle(bytes).unpickle[t]

  val UNVERSIONED = 0
  val ENABLED = 1
  val SUSPENDED = 2
}
