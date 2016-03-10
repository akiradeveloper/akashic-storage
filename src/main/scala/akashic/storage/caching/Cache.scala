package akashic.storage.caching

import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{Files, Path}

import akashic.storage.files

trait Cache[V] {
  type K = String
  def k: K = {
    val attr = Files.readAttributes(file, classOf[BasicFileAttributes])
    s"${attr.fileKey.hashCode}-${attr.creationTime.toMillis}"
  }
  def file: Path
  def cacheMap: CacheMap[K, V]
  def reader: Array[Byte] => V
  def writer: V => Array[Byte]
  def get: V = {
    cacheMap.find(k) match {
      case Some(a) => a
      case None =>
        val bytes = files.readBytes(file)
        val ret = reader(bytes)
        cacheMap.insert(k, ret)
        ret
    }
  }
  def put(v: V) {
    cacheMap.find(k) match {
      case None =>
        val bytes = writer(v)
        files.writeBytes(file, bytes)
        cacheMap.insert(k, v)
      case _ =>
    }
  }
}
