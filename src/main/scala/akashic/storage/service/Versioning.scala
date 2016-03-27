package akashic.storage.service

import akashic.storage.backend.NodePath
import akashic.storage.caching.{CacheMap, Cache}
import scala.pickling.Defaults._
import scala.pickling.binary._
import akashic.storage.server

import Versioning._
case class Versioning(value: Int) {
  def toBytes: Array[Byte] = this.pickle.value
  def enabled = value == ENABLED
}
object Versioning {
  type t = Versioning
  def makeCache(path: NodePath) = new Cache[t] {
    override def cacheMap: CacheMap[K, t] = server.cacheMaps.forVersioning
    override def writer: (t) => Array[Byte] = Versioning.writer
    override def reader: (Array[Byte]) => t = Versioning.reader
    override val filePath = path
  }
  def writer(a: t) = a.toBytes
  def reader(a: Array[Byte]): t = fromBytes(a)
  def fromBytes(bytes: Array[Byte]): t = BinaryPickle(bytes).unpickle[Versioning]
  val UNVERSIONED = 0
  val ENABLED = 1
  val SUSPENDED = 2
}

