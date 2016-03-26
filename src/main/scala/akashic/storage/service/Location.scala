package akashic.storage.service

import java.nio.file.Path

import akashic.storage.backend.NodePath
import akashic.storage.caching.{CacheMap, Cache}
import com.google.common.cache.CacheBuilder

import scala.pickling.Defaults._
import scala.pickling.binary._
import akashic.storage.server

object Location {
  def writer(a: t) = a.toBytes
  def reader(a: Array[Byte]) = fromBytes(a)
  def makeCache(path: NodePath) = new Cache[Location.t] {
    override def cacheMap: CacheMap[K, Location.t] = server.cacheMaps.forLocation
    override def writer: (Location.t) => Array[Byte] = Location.writer
    override def reader: (Array[Byte]) => Location.t = Location.reader
    override val filePath = path
  }
  case class t(value: String) {
    def toBytes: Array[Byte] = this.pickle.value
    def toXML = <LocationConstraint>{value}</LocationConstraint>
  }
  def fromBytes(bytes: Array[Byte]): t = BinaryPickle(bytes).unpickle[t]
}
