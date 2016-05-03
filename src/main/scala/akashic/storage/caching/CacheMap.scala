package akashic.storage.caching

trait CacheMap[V] {
  def find(k: Cache.Key): Option[V]
  def insert(k: Cache.Key, v: V)
}

object CacheMap {
  case class Null[V]() extends CacheMap[V] {
    def find(k: Cache.Key): Option[V] = None
    def insert(k: Cache.Key, v: V) = {}
  }
  case class Guava[V](backing: com.google.common.cache.Cache[Cache.Key, V]) extends CacheMap[V] {
    override def find(k: Cache.Key): Option[V] = backing.getIfPresent(k) match {
      case null => None
      case a => Some(a)
    }
    override def insert(k: Cache.Key, v: V): Unit = {
      backing.put(k, v)
    }
  }
}

