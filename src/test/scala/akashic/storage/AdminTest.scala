package akashic.storage

import akashic.storage.admin.User
import org.apache.commons.codec.binary.Base64
import org.apache.http.client.entity.EntityBuilder
import org.apache.http.client.methods.{HttpPut, HttpGet, HttpPost}
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicHeader

import scala.xml.XML

class AdminTest extends ServerTestBase {
  case class FixtureParam()
  override protected def withFixture(test: OneArgTest) = {
    test(FixtureParam())
  }

  def rootURL = s"http://${server.address}/admin/user"
  val authHeader = new BasicHeader("Authorization", s"Basic ${Base64.encodeBase64URLSafeString("admin:passwd".getBytes)}")

  test("post -> get") { p =>
    // POST
    val postReq = new HttpPost(rootURL)
    postReq.addHeader(authHeader)

    val postRes = HttpClients.createDefault.execute(postReq)
    assert(postRes.getStatusLine.getStatusCode === 200)
    val user = User.fromXML(XML.load(postRes.getEntity.getContent))

    // GET
    val getReq = new HttpGet(s"${rootURL}/${user.id}")
    getReq.addHeader(authHeader)

    val getRes = HttpClients.createDefault.execute(getReq)
    assert(getRes.getStatusLine.getStatusCode === 200)
    val gotUser = User.fromXML(XML.load(getRes.getEntity.getContent))
    assert(user === gotUser)
  }

  test("post -> put -> get") { p =>
    // POST
    val postReq = new HttpPost(rootURL)
    postReq.addHeader(authHeader)

    val postRes = HttpClients.createDefault.execute(postReq)
    assert(postRes.getStatusLine.getStatusCode === 200)
    val user = User.fromXML(XML.load(postRes.getEntity.getContent))
    assert(user.name !== "hige")
    assert(user.email !== "hige@hige.net")

    // PUT
    val xml =
      <User>
        <Name>hige</Name>
        <Email>hige@hige.net</Email>
      </User>
    val putReq = new HttpPut(s"${rootURL}/${user.id}")
    putReq.addHeader(authHeader)
    putReq.setEntity(EntityBuilder.create.setText(xml.toString).build)

    val putRes = HttpClients.createDefault.execute(putReq)
    assert(putRes.getStatusLine.getStatusCode === 200)

    // GET
    val getReq = new HttpGet(s"${rootURL}/${user.id}")
    getReq.addHeader(authHeader)

    val getRes = HttpClients.createDefault.execute(getReq)
    assert(postRes.getStatusLine.getStatusCode === 200)
    val gotUser = User.fromXML(XML.load(getRes.getEntity.getContent))
    assert(gotUser.name === "hige")
    assert(gotUser.email === "hige@hige.net")
  }
}
