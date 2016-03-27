package akashic.storage.service

import java.nio.file.Path

import akashic.storage.HeaderList
import akashic.storage.backend.NodePath
import akashic.storage.caching.CacheMap.Guava
import akashic.storage.caching.{CacheMap, Cache}
import com.google.common.cache.CacheBuilder

import scala.pickling.Defaults._
import scala.pickling.binary._
import akashic.storage.server

object Meta {
  def writer(a: Meta): Array[Byte] = a.toBytes
  def reader(a: Array[Byte]) = fromBytes(a)
  def makeCache(path: NodePath) = new Cache[Meta] {
    override def cacheMap: CacheMap[K, Meta] = server.cacheMaps.forMeta
    override def writer: (Meta) => Array[Byte] = Meta.writer
    override def reader: (Array[Byte]) => Meta = Meta.reader
    override val filePath = path
  }

  def fromBytes(bytes: Array[Byte]): Meta = {
    BinaryPickle(bytes).unpickle[Meta]
  }
}
case class Meta(isVersioned: Boolean,
                isDeleteMarker: Boolean,
                eTag: String,
                attrs: HeaderList.t,
                xattrs: HeaderList.t) {
  def toBytes: Array[Byte] = this.pickle.value
}
