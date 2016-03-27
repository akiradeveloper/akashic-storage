package akashic.storage.caching

import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{Files, Path}

import akashic.storage.patch.Data

trait Cache[V] extends Data[V] {
  /** not to cache */
  val NULL_KEY = ""
  type K = String
  private def computeLookupKey: K = {
    filePath.exists match {
      case true =>
        val attr = filePath.getAttr
        attr.cacheKey match {
          // if the fileKey is null we can't safely cache objects.
          // because the filesystem returns null fileKey is not common case
          // I decided to not cache.
          case None => NULL_KEY
          case Some(key) => s"${key}-${attr.creationTime}"
        }
      case false => NULL_KEY
    }
  }
  private def k: K = {
    cacheMap match {
      case _: CacheMap.Null[K, V] => NULL_KEY
      case _ => computeLookupKey
    }
  }
  def cacheMap: CacheMap[K, V]
  def reader: Array[Byte] => V
  def writer: V => Array[Byte]
  def get: V = {
    cacheMap.find(k) match {
      case Some(a) => a
      case None =>
        val bytes = filePath.readFile
        val ret = reader(bytes)
        cacheMap.insert(k, ret)
        ret
    }
  }
  def put(v: V) {
    cacheMap.find(k) match {
      case None =>
        val bytes = writer(v)
        filePath.createFile(bytes)
        cacheMap.insert(k, v)
      case _ =>
    }
  }
}
