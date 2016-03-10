package akashic.storage.service

import java.nio.file.Path

import akashic.storage.HeaderList
import akashic.storage.caching.{CacheMap, Cache}

import scala.pickling.Defaults._
import scala.pickling.binary._

object Meta {
  def writer(a: t): Array[Byte] = a.toBytes
  def reader(a: Array[Byte]) = fromBytes(a)
  def makeCache(path: Path) = new Cache[Meta.t] {
    override def cacheMap: CacheMap[K, t] = new CacheMap[K, Meta.t]()
    override def writer: (t) => Array[Byte] = Meta.writer
    override def reader: (Array[Byte]) => t = Meta.reader
    override val filePath: Path = path
  }
  case class t(isVersioned: Boolean,
               isDeleteMarker: Boolean,
               eTag: String,
               attrs: HeaderList.t,
               xattrs: HeaderList.t) {
    def toBytes: Array[Byte] = this.pickle.value
  }
  def fromBytes(bytes: Array[Byte]): t = {
    BinaryPickle(bytes).unpickle[t]
  }
}
