package akashic.storage

import java.io.ByteArrayInputStream

import org.scalatest.Outcome

import scala.sys.process.Process

class AdminCLITest extends ServerTestBase {
  case class FixtureParam()
  override protected def withFixture(test: OneArgTest): Outcome = {
    test(FixtureParam())
  }

  override def beforeEach(): Unit = {
    super.beforeEach
    val input = "localhost\n10946\npasswd"
    val is = new ByteArrayInputStream(input.getBytes)
    val cmd = Process("akashic-admin-config")
    val res = (cmd #< is).!
    assert(res === 0)
  }

  test("add") { _ =>
    val cmd = Process("akashic-admin-add")
    val res = cmd.!!
    println(res)
    assert(res.length === 120)
  }

  test("add and get") { _ =>
    val add = Process("akashic-admin-add")
    val userId = add.!!
    val get = Process(s"akashic-admin-get ${userId}")
    assert(get.! === 0)
  }
}
