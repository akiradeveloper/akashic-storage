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
    assert(res.length === 65)
  }

  test("get") { _ =>
    val add = Process("akashic-admin-add")
    val userId = add.!!
    println(userId)
    val get = Process(s"akashic-admin-get ${userId}")
    assert(get.! === 0)
  }

  test("list") { _ =>
    val res0 = Process("akashic-admin-list").!!
    Process("akashic-admin-add").!
    val res1 = Process("akashic-admin-list").!!
    assert(res0.length < res1.length)
    Process("akashic-admin-add").!
    val res2 = Process("akashic-admin-list").!!
    assert(res1.length < res2.length)
  }

  test("update") { _ =>
    val add = Process("akashic-admin-add")
    val userId = add.!!
    println(userId)
    val update = Process(s"akashic-admin-update -name=abc -email=abc@abc.com ${userId}")
    assert(update.! === 0)
  }

  test("setup-mc") { _ =>
    val add = Process("akashic-admin-add")
    val userId = add.!!

    val input = "aaa\n10000\n"
    val is = new ByteArrayInputStream(input.getBytes)
    val setupMc = Process(s"akashic-admin-setup-mc ${userId}")
    val res = (setupMc #< is).!

    assert(res === 0)
  }
}
