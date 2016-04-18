package akashic.storage

import java.io.File
import java.nio.file.{Paths, Files}

import com.typesafe.config.{ConfigFactory, Config}
import org.apache.commons.io.FileUtils
import org.scalatest.{Suites, BeforeAndAfterEach, FunSuite}

abstract class BALTraitTest(config: Config) extends FunSuite with BeforeAndAfterEach {
  import akashic.storage.backend._
  var bal: BAL = _
  override def beforeEach(): Unit = {
    bal = new BALFactory(config).build
  }
  test("add directory") {
    bal.makeDirectory(bal.getRoot, "aaa")
    val dir = bal.lookup(bal.getRoot, "aaa").get
    bal.makeDirectory(dir, "bbb")
  }
}

class LocalTest extends BALTraitTest(ConfigFactory.parseResources("backend-local.conf").getConfig("backend")) {
  override def beforeEach() = {
    FileUtils.cleanDirectory(new File("/tmp/l"))
    Files.createDirectories(Paths.get("/tmp/l"))
    super.beforeEach()
  }
}

class MemoryTest extends BALTraitTest(ConfigFactory.parseResources("backend-memory.conf").getConfig("backend"))

//class BALTraitTests extends Suites (
//  new LocalTest,
//  new MemoryTest
//)
