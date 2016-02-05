package akashic.storage

import java.io.{FileInputStream, File}
import java.nio.file.{Files, Path}

import com.amazonaws.services.s3.model.S3Object
import com.typesafe.config._
import org.apache.commons.io.IOUtils
import org.scalatest._
import akashic.storage.admin.TestUsers

import scala.util.Random

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

  def createLargeFile(path: Path, sizeMB: Int): Unit = {
    if (!Files.exists(path)) {
      val result = new Array[Byte](sizeMB * 1024 * 1024)
      new Random().nextBytes(result)
      files.writeBytes(path, result)
    }
  }

  def checkFileContent(actual: S3Object, expected: File) {
    assert(IOUtils.contentEquals(actual.getObjectContent, new FileInputStream(expected)))
  }
}
