package akasha

import akasha.admin.User
import akasha.http._

import scala.xml.XML
import scalaj.http.Http

class AdminTest extends ServerTestBase {
  case class FixtureParam()
  override protected def withFixture(test: OneArgTest) = {
    test(FixtureParam())
  }

  val rootURL = "http://localhost:9000/admin/user"

  test("post and get") { p =>
    val postRes = Http(rootURL).method("POST").asString
    assert(postRes.code === 200)
    val user = User.fromXML(XML.loadString(postRes.body))
    val getRes = Http(s"${rootURL}/${user.id}").method("GET").asString
    assert(postRes.code === 200)
    val gotUser = User.fromXML(XML.loadString(getRes.body))
    assert(user === gotUser)
  }

  test("add -> put -> get") { p =>
  }
}
