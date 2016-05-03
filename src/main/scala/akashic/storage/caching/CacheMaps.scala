package akashic.storage.caching

import akashic.storage.ServerConfig
import akashic.storage.service.{Acl, Location, Meta, Versioning}
import com.google.common.cache.CacheBuilder

case class CacheMaps(config: ServerConfig) {
  val forVersioning = new CacheMap.Guava[Versioning](
    CacheBuilder.newBuilder
      .maximumSize(32)
      .build())
  val forLocation = new CacheMap.Guava[Location](
    CacheBuilder.newBuilder
      .maximumSize(32)
      .build())
  val forAcl = new CacheMap.Guava[Acl](
    CacheBuilder.newBuilder
      .maximumSize(2048)
      .build())
  val forMeta = new CacheMap.Guava[Meta](
    CacheBuilder.newBuilder
      .maximumSize(2048)
      .build()
  )
}
