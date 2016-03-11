package akashic.storage.caching

import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{Files, Path}

import akashic.storage.files
import akashic.storage.patch.Data

trait Cache[V] extends Data[V] {
  val NULL_KEY = ""
  type K = String
  private def computeLookupKey: K = {
    Files.exists(filePath) match {
      case true =>
        val attr = Files.readAttributes(filePath, classOf[BasicFileAttributes])
        attr.fileKey match {
          // if the fileKey is null we can't safely cache objects.
          // because the filesystem returns null fileKey is not common case
          // I decided to not cache.
          case null => NULL_KEY
          case x => s"${x.hashCode}-${attr.creationTime.toMillis}"
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
        val bytes = files.readBytes(filePath)
        val ret = reader(bytes)
        cacheMap.insert(k, ret)
        ret
    }
  }
  def put(v: V) {
    cacheMap.find(k) match {
      case None =>
        val bytes = writer(v)
        files.writeBytes(filePath, bytes)
        cacheMap.insert(k, v)
      case _ =>
    }
  }
}
