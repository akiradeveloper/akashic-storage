package akashic.storage.caching

import akashic.storage.backend.NodePath
import akashic.storage.patch.{DirectoryPath, Data}

object Cache {
  type Key = (String, Long)
  def creationTimeOf(dir: DirectoryPath, name: String): Long = {
    if (!dir.exists)
      return 0
    if (!dir(name).exists)
      return 0
    dir(name).getAttr.creationTime
  }
  def creationTimeOf(f: NodePath): Long = {
    if (!f.exists)
      return 0
    f.getAttr.creationTime
  }
}

trait Cache[V] extends Data[V] {
  /** not to cache */
  val NULL_KEY = ("", 0L)
  private def computeLookupKey: Cache.Key = {
    filePath.exists match {
      case true =>
        val attr = filePath.getAttr
        (filePath.path, attr.creationTime)
      case false =>
        NULL_KEY
    }
  }
  private def k: Cache.Key = computeLookupKey
  def cacheMap: CacheMap[V]
  def reader: Array[Byte] => V
  def writer: V => Array[Byte]
  override def get: V = {
    val key = k
    cacheMap.find(key) match {
      case Some(a) =>
        a
      case None =>
        val bytes = filePath.readFile
        val ret = reader(bytes)
        cacheMap.insert(key, ret)
        ret
    }
  }
  override def replace(v: V, creTime: Long) {
    val bytes = writer(v)
    filePath.createFile(bytes)

    while (filePath.getAttr.creationTime < creTime + 1000) {
      filePath.removeIfExists
      Thread.sleep(1000)
      filePath.createFile(bytes)
    }
  }
}
