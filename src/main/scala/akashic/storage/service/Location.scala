package akashic.storage.service

import akashic.storage.backend.NodePath
import akashic.storage.caching.{Cache, CacheMap}
import akashic.storage.server

import scala.pickling.Defaults._
import scala.pickling.binary._

case class Location(value: String) {
  def toBytes: Array[Byte] = this.pickle.value
  def toXML = <LocationConstraint>{value}</LocationConstraint>
}
object Location {
  type t = Location
  def writer(a: t) = a.toBytes
  def reader(a: Array[Byte]) = fromBytes(a)
  def makeCache(path: NodePath) = new Cache[t] {
    override def cacheMap: CacheMap[t] = server.cacheMaps.forLocation
    override def writer: (t) => Array[Byte] = Location.writer
    override def reader: (Array[Byte]) => t = Location.reader
    override val filePath = path
  }
  def fromBytes(bytes: Array[Byte]): t = BinaryPickle(bytes).unpickle[t]
}

