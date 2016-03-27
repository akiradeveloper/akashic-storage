package akashic.storage.caching

import akashic.storage.ServerConfig
import akashic.storage.service.{Location, Meta, Acl, Versioning}
import com.google.common.cache.CacheBuilder

case class CacheMaps(config: ServerConfig) {
  val forVersioning = new CacheMap.Guava[String, Versioning.t](
    CacheBuilder.newBuilder
      .maximumSize(32)
      .build())
  val forLocation = new CacheMap.Guava[String, Location.t](
    CacheBuilder.newBuilder
      .maximumSize(32)
      .build())
  val forAcl = new CacheMap.Guava[String, Acl.t](
    CacheBuilder.newBuilder
      .maximumSize(2048)
      .build())
  val forMeta = new CacheMap.Guava[String, Meta](
    CacheBuilder.newBuilder
      .maximumSize(2048)
      .build()
  )
}
