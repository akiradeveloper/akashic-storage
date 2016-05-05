package akashic.storage.service

import akashic.storage.backend.NodePath
import akashic.storage.caching.{Cache, CacheMap}
import akashic.storage.{HeaderList, server}

import scala.pickling.Defaults._
import scala.pickling.binary._

case class Meta(versionId: String,
                eTag: String,
                attrs: HeaderList,
                xattrs: HeaderList) {
  def toBytes: Array[Byte] = this.pickle.value
  def isDeleteMarker = eTag == Meta.DELETE_MARKER
  def versionType(versionIndex: Int): Versioning = {
    if (versionId != Meta.DELETE_MARKER) {
      Versioning.ENABLED
    } else {
      if (versionIndex > 0) {
        Versioning.SUSPENDED
      } else {
        Versioning.UNVERSIONED
      }
    }
  }
}
object Meta {
  val DELETE_MARKER = ""
  type t = Meta
  def writer(a: t): Array[Byte] = a.toBytes
  def reader(a: Array[Byte]) = fromBytes(a)
  def makeCache(path: NodePath) = new Cache[t] {
    override def cacheMap: CacheMap[t] = server.cacheMaps.forMeta
    override def writer: (t) => Array[Byte] = Meta.writer
    override def reader: (Array[Byte]) => t = Meta.reader
    override val filePath = path
  }
  def fromBytes(bytes: Array[Byte]): t = {
    BinaryPickle(bytes).unpickle[Meta]
  }
}
