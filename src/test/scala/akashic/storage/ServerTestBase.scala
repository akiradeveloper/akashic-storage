package akashic.storage

import com.twitter.finagle.{NullServer, ListeningServer}
import com.typesafe.config._
import org.scalatest._
import akashic.storage.admin.TestUsers

abstract class ServerTestBase extends fixture.FunSuite with BeforeAndAfterEach {
  def makeConfig = ServerConfig.forConfig(ConfigFactory.load("test.conf"))
  var server: Server = _
  var finagleServer: ListeningServer = NullServer

  override def beforeEach {
    val config = makeConfig
    server = Server(config)
    finagleServer = server.run

    // FIXME (should via HTTP)
    server.users.addUser(TestUsers.hoge)

    // Await.ready(finagleServer)
  }

  override def afterEach {
    // Await.ready(finagleServer.close())
    finagleServer.close()
    finagleServer = NullServer
  }
}
