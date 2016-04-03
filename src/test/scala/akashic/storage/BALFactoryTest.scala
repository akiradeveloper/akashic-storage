package akashic.storage

import java.nio.file.{Paths, Files, Path}

import akashic.storage.backend.BALFactory
import com.typesafe.config.ConfigFactory
import org.scalatest.FunSuite

class BALFactoryTest extends FunSuite {
  test("read test config to build local fs") {
    val fs = new BALFactory(ConfigFactory.parseResources("backend-local.conf").getConfig("backend")).build
    Files.createDirectories(Paths.get("/tmp/l"))
    val root = fs.getRoot.asInstanceOf[Path]
    assert(root === Paths.get("/tmp/l"))
  }
}
