package akashic.storage

import java.io.{FileInputStream, File}
import java.nio.file.{Files, Path, Paths}

import com.amazonaws.services.s3.model.S3Object
import com.twitter.finagle.{NullServer, ListeningServer}
import com.typesafe.config._
import org.apache.commons.io.IOUtils
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

  def getTestFile(name: String): File = {
    val loader = getClass.getClassLoader
    new File(loader.getResource(name).getFile)
  }

  val LARGE_FILE_PATH = Paths.get("/tmp/akashic-storage-test-large-file")
  def createLargeFile(path: Path): Unit = {
    if (!Files.exists(path)) {
      files.writeBytes(path, strings.random(32 * 1024 * 1024).map(_.toByte).toArray)
    }
  }

  def checkFileContent(actual: S3Object, expected: File) {
    assert(IOUtils.contentEquals(actual.getObjectContent, new FileInputStream(expected)))
  }
}
