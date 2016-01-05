package akasha

import akasha.http._

class AdminTest extends ServerTestBase {
  case class FixtureParam()
  override protected def withFixture(test: OneArgTest) = {
    test(FixtureParam())
  }

  test("add and get") { p =>
  }

  test("add -> put -> get") { p =>
  }
}
