package akashic.storage.backend

import com.typesafe.config.Config
import org.scalatest.{BeforeAndAfterEach, FunSuite}

trait BALTraitTest extends FunSuite with BeforeAndAfterEach {
  val config: Config
  var bal: BAL = _
  override def beforeEach(): Unit = {
    super.beforeEach()
    bal = new BALFactory(config).build
  }
  test("add directory") {
    bal.makeDirectory(bal.getRoot, "aaa")
    val dir = bal.lookup(bal.getRoot, "aaa").get
    bal.makeDirectory(dir, "bbb")
  }
}

abstract class BALTraitTestTemplate(val config: Config) extends BALTraitTest
