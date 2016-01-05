package akasha

import akasha.http._
import com.twitter.finagle.{NullServer, ListeningServer}
import com.twitter.util.Await
import com.typesafe.config._
import org.scalatest._

abstract class ServerTestBase extends fixture.FunSuite with BeforeAndAfterEach {
  val config = ServerConfig.forConfig(ConfigFactory.load("test.conf"))
  var server: Server = _
  var finagleServer: ListeningServer = NullServer

  override def beforeEach {
    server = Server(config)
    finagleServer = server.run
    // Await.ready(finagleServer)
  }

  override def afterEach {
    // Await.ready(finagleServer.close())
    finagleServer.close()
  }
}
