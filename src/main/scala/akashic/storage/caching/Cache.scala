package akashic.storage.caching

import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{Files, Path}

import akashic.storage.files
import akashic.storage.patch.Data

trait Cache[V] extends Data[V] {
  type K = String
  def k: K = {
    val attr = Files.readAttributes(filePath, classOf[BasicFileAttributes])
    s"${attr.fileKey.hashCode}-${attr.creationTime.toMillis}"
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
    val lookupKey = Files.exists(filePath) match {
      case true => k
      case false => ""
    }
    cacheMap.find(lookupKey) match {
      case None =>
        val bytes = writer(v)
        files.writeBytes(filePath, bytes)
        cacheMap.insert(k, v)
      case _ =>
    }
  }
}
