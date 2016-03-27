package akashic.storage.service

import java.nio.file.Path

import akashic.storage.backend.NodePath
import akashic.storage.caching.{CacheMap, Cache}
import com.google.common.cache.CacheBuilder

import scala.pickling.Defaults._
import scala.pickling.binary._
import akashic.storage.server

case class Location(value: String) {
  def toBytes: Array[Byte] = this.pickle.value
  def toXML = <LocationConstraint>{value}</LocationConstraint>
}
object Location {
  type t = Location
  def writer(a: t) = a.toBytes
  def reader(a: Array[Byte]) = fromBytes(a)
  def makeCache(path: NodePath) = new Cache[t] {
    override def cacheMap: CacheMap[K, t] = server.cacheMaps.forLocation
    override def writer: (t) => Array[Byte] = Location.writer
    override def reader: (Array[Byte]) => t = Location.reader
    override val filePath = path
  }
  def fromBytes(bytes: Array[Byte]): t = BinaryPickle(bytes).unpickle[t]
}

