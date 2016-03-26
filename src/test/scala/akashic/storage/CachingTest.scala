package akashic.storage

import java.nio.charset.Charset
import java.nio.file.Paths

import akashic.storage.backend.NodePath
import akashic.storage.caching.{CacheMap, Cache}
import com.google.common.cache.CacheBuilder

class CachingTest extends ServerTestBase {
  case class FixtureParam()
  override protected def withFixture(test: OneArgTest) = {
    test(FixtureParam())
  }

  val guava = new CacheMap.Guava[String, String](
    CacheBuilder.newBuilder
      .maximumSize(32)
      .build())

  var cache: Cache[String] = _

  override def beforeEach: Unit = {
    super.beforeEach

    guava.backing.invalidateAll()

    cache = new Cache[String] {
      val UTF8 = Charset.forName("UTF-8")
      override val filePath = NodePath(Paths.get("/tmp"), "t", None)
      override def writer: (String) => Array[Byte] = (a: String) => a.getBytes(UTF8)
      override def reader: (Array[Byte]) => String = (a: Array[Byte]) => new String(a, UTF8)
      override def cacheMap: CacheMap[K, String] = guava
    }
  }

  override def afterEach = {
    super.afterEach
  }

  test("simple get") { _ =>
    cache.put("hoge")
    val r0 = cache.get
    assert(r0 === "hoge")
    val r1 = cache.get
    assert(r1 === "hoge")
  }

  test("invalidate and get") { _ =>
    cache.put("hige")
    assert(cache.get === "hige")
    guava.backing.invalidateAll()
    assert(cache.get === "hige")
  }
}
