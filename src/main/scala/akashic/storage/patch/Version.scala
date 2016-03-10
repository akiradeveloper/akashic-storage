package akashic.storage.patch

import java.nio.file.Path

import akashic.storage.caching.{CacheMap, Cache}
import akashic.storage.service.{Acl, Meta}

case class Version(root: Path) extends Patch {
  def key = Key(root.getParent.getParent)
  val data = Data(root.resolve("data"))
  val meta = Data(root.resolve("meta"))
  // val acl = Data(root.resolve("acl"))
  val acl = new Cache[Acl.t] {
    val data = Data(file)
    override def file: Path = root.resolve("acl")
    override def writer: (Acl.t) => Array[Byte] = Acl.writer
    override def reader: (Array[Byte]) => Acl.t = Acl.reader
    override def cacheMap: CacheMap[K, Acl.t] = new CacheMap[K, Acl.t]()
  }

  // [spec] All objects (including all object versions and delete markers)
  // in the bucket must be deleted before the bucket itself can be deleted.
  val deletable = false
}
