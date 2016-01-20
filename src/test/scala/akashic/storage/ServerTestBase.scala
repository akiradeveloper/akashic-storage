package akashic.storage

import com.twitter.finagle.{NullServer, ListeningServer}
import com.typesafe.config._
import org.scalatest._
import akashic.storage.admin.TestUsers

abstract class ServerTestBase extends fixture.FunSuite with BeforeAndAfterEach {
  def makeConfig = ServerConfig.forConfig(ConfigFactory.load("test.conf"))

  override def beforeEach {
    val config = makeConfig
    server = Server(config)
    server.start

    // FIXME (should via HTTP)
    server.users.addUser(TestUsers.hoge)

    // Await.ready(finagleServer)
  }

  override def afterEach {
    server.stop
  }
}
