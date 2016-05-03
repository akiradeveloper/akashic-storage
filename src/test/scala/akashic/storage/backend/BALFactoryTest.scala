package akashic.storage.backend

import java.nio.file.{Files, Path, Paths}

import com.typesafe.config.ConfigFactory
import org.scalatest.FunSuite

class BALFactoryTest extends FunSuite {
  test("read test config to build local fs") {
    val fs = new BALFactory(ConfigFactory.parseResources("backend-local.conf").getConfig("backend")).build
    val root = fs.getRoot.asInstanceOf[Path]
    assert(root === Paths.get("/mnt/akashic-storage"))
  }
}
