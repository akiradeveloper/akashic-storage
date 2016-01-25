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
  def makeConfig = ServerConfig(ConfigFactory.load("test.conf"), init = true)

  override def beforeEach {
    val config = makeConfig
    server = Server(config)
    server.start

    // FIXME (should via HTTP)
    server.users.addUser(TestUsers.hoge)
  }

  override def afterEach {
    server.stop
  }

  def getTestFile(name: String): File = {
    val loader = getClass.getClassLoader
    new File(loader.getResource(name).getFile)
  }

  val FILE_PATH_8MB = Paths.get("/tmp/akashic-storage-test-file-8mb")
  val FILE_PATH_32MB = Paths.get("/tmp/akashic-storage-test-file-32mb")
  def createLargeFile(path: Path, sizeMB: Int): Unit = {
    if (!Files.exists(path)) {
      files.writeBytes(path, strings.random(sizeMB * 1024 * 1024).map(_.toByte).toArray)
    }
  }

  def checkFileContent(actual: S3Object, expected: File) {
    assert(IOUtils.contentEquals(actual.getObjectContent, new FileInputStream(expected)))
  }
}
