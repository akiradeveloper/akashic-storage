package akashic.storage

import java.nio.file.{Paths, Files, Path}

import akashic.storage.backend.BackendFactory
import com.typesafe.config.ConfigFactory
import org.scalatest.FunSuite

class BackendFactoryTest extends FunSuite {
  test("read test config to build local fs") {
    val fs = new BackendFactory(ConfigFactory.parseResources("backend-local.conf")).build
    val root = fs.getRoot.asInstanceOf[Path]
    assert(root === Paths.get("/tmp/l"))
  }
}
