package akashic.storage

import java.nio.charset.Charset
import java.nio.file.{Paths, Path}

import akashic.storage.caching.{CacheMap, Cache}
import com.google.common.cache.CacheBuilder
import org.scalatest.{BeforeAndAfterEach, FunSuite}

class CachingTest extends FunSuite with BeforeAndAfterEach {

  val guava = new CacheMap.Guava[String, String](
    CacheBuilder.newBuilder
      .maximumSize(32)
      .build())

  var cache: Cache[String] = _

  override def beforeEach: Unit = {
    guava.backing.invalidateAll()

    cache = new Cache[String] {
      val UTF8 = Charset.forName("UTF-8")
      override val filePath: Path = Paths.get("/tmp/t")
      override def writer: (String) => Array[Byte] = (a: String) => a.getBytes(UTF8)
      override def reader: (Array[Byte]) => String = (a: Array[Byte]) => new String(a, UTF8)
      override def cacheMap: CacheMap[K, String] = guava
    }
  }

  test("simple get") {
    cache.put("hoge")
    val r0 = cache.get
    assert(r0 === "hoge")
    val r1 = cache.get
    assert(r1 === "hoge")
  }

  test("invalidate and get") {
    cache.put("hige")
    assert(cache.get === "hige")
    guava.backing.invalidateAll()
    assert(cache.get === "hige")
  }
}
