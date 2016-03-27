package akashic.storage.service

import java.nio.file.Path

import akashic.storage.backend.NodePath
import akashic.storage.caching.{CacheMap, Cache}
import com.google.common.cache.CacheBuilder

import scala.pickling.Defaults._
import scala.pickling.binary._
import akashic.storage.server

object Versioning {
  def makeCache(path: NodePath) = new Cache[Versioning] {
    override def cacheMap: CacheMap[K, Versioning] = server.cacheMaps.forVersioning
    override def writer: (Versioning) => Array[Byte] = Versioning.writer
    override def reader: (Array[Byte]) => Versioning = Versioning.reader
    override val filePath = path
  }
  def writer(a: Versioning) = a.toBytes
  def reader(a: Array[Byte]): Versioning = fromBytes(a)
  def fromBytes(bytes: Array[Byte]): Versioning = BinaryPickle(bytes).unpickle[Versioning]

  val UNVERSIONED = 0
  val ENABLED = 1
  val SUSPENDED = 2
}
import Versioning._
case class Versioning(value: Int) {
  def toBytes: Array[Byte] = this.pickle.value
  def enabled = value == ENABLED
}
