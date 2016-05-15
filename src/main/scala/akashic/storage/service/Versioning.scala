package akashic.storage.service

import akashic.storage.backend.NodePath
import akashic.storage.caching.{Cache, CacheMap}
import akashic.storage.server

import scala.pickling.Defaults._
import scala.pickling.binary._
case class Versioning(value: Int) {
  def toBytes: Array[Byte] = this.pickle.value
  def toXML = {
    value match {
      case 0 => <VersioningConfiguration/>
      case 1 => <VersioningConfiguration>Enabled</VersioningConfiguration>
      case 2 => <VersioningConfiguration>Suspended</VersioningConfiguration>
    }
  }
}
object Versioning {
  val UNVERSIONED = Versioning(0)
  val ENABLED = Versioning(1)
  val SUSPENDED = Versioning(2)

  type t = Versioning
  def makeCache(path: NodePath) = new Cache[t] {
    override def cacheMap: CacheMap[t] = server.cacheMaps.forVersioning
    override def writer: (t) => Array[Byte] = Versioning.writer
    override def reader: (Array[Byte]) => t = Versioning.reader
    override val filePath = path
  }
  def writer(a: t) = a.toBytes
  def reader(a: Array[Byte]): t = fromBytes(a)
  def fromBytes(bytes: Array[Byte]): t = BinaryPickle(bytes).unpickle[Versioning]
}
