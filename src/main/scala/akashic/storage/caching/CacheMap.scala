package akashic.storage.caching

trait CacheMap[K, V] {
  def find(k: K): Option[V]
  def insert(k: K, v: V)
}

object CacheMap {
  class Null[K, V] extends CacheMap[K, V] {
    def find(k: K): Option[V] = None
    def insert(k: K, v: V) = {}
  }
  class Guava[K, V](backing: com.google.common.cache.Cache[K, V]) extends CacheMap[K, V] {
    override def find(k: K): Option[V] = backing.getIfPresent(k) match {
      case null => None
      case a => Some(a)
    }
    override def insert(k: K, v: V): Unit = {
      backing.put(k, v)
    }
  }
}

