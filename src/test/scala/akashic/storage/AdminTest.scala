package akashic.storage

import akashic.storage.admin.User

import scala.xml.XML
import scalaj.http.Http

class AdminTest extends ServerTestBase {
  case class FixtureParam()
  override protected def withFixture(test: OneArgTest) = {
    test(FixtureParam())
  }

  def rootURL = s"http://${server.address}/admin/user"

  test("post -> get") { p =>
    val postRes = Http(rootURL).method("POST").asString
    assert(postRes.code === 200)
    val user = User.fromXML(XML.loadString(postRes.body))
    val getRes = Http(s"${rootURL}/${user.id}").method("GET").asString
    assert(postRes.code === 200)
    val gotUser = User.fromXML(XML.loadString(getRes.body))
    assert(user === gotUser)
  }

  test("post -> put -> get") { p =>
    val postRes = Http(rootURL).method("POST").asString
    assert(postRes.code === 200)
    val user = User.fromXML(XML.loadString(postRes.body))
    assert(user.name !== "hige")
    assert(user.email !== "hige@hige.net")

    val xml =
      <User>
        <Name>hige</Name>
        <Email>hige@hige.net</Email>
      </User>
    val putRes = Http(s"${rootURL}/${user.id}").postData(xml.toString).method("PUT").asString
    assert(putRes.code === 200)

    val getRes = Http(s"${rootURL}/${user.id}").method("GET").asString
    assert(postRes.code === 200)
    val gotUser = User.fromXML(XML.loadString(getRes.body))
    assert(gotUser.name === "hige")
    assert(gotUser.email === "hige@hige.net")
  }
}
