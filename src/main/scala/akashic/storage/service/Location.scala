package akashic.storage.service

import java.nio.file.Path

import akashic.storage.backend.NodePath
import akashic.storage.caching.{CacheMap, Cache}
import com.google.common.cache.CacheBuilder

import scala.pickling.Defaults._
import scala.pickling.binary._
import akashic.storage.server

object Location {
  def writer(a: Location) = a.toBytes
  def reader(a: Array[Byte]) = fromBytes(a)
  def makeCache(path: NodePath) = new Cache[Location] {
    override def cacheMap: CacheMap[K, Location] = server.cacheMaps.forLocation
    override def writer: (Location) => Array[Byte] = Location.writer
    override def reader: (Array[Byte]) => Location = Location.reader
    override val filePath = path
  }
  def fromBytes(bytes: Array[Byte]): Location = BinaryPickle(bytes).unpickle[Location]
}
case class Location(value: String) {
  def toBytes: Array[Byte] = this.pickle.value
  def toXML = <LocationConstraint>{value}</LocationConstraint>
}
