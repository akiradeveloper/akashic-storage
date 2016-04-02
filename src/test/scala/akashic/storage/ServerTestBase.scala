package akashic.storage

import java.io.{FileInputStream, File}
import java.nio.file.{Paths, Files, Path}

import com.amazonaws.services.s3.model.S3Object
import com.typesafe.config._
import org.apache.commons.io.IOUtils
import org.scalatest._
import akashic.storage.admin.TestUsers

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Random

abstract class ServerTestBase extends fixture.FunSuite with BeforeAndAfterEach {
  def makeConfig = ServerConfig.fromConfig(ConfigFactory.load("test.conf"))

  override def beforeEach {
    val config = makeConfig
    val mountpoint = Paths.get(config.rawConfig.getConfig("backend").getString("mountpoint"))
    Files.createDirectories(mountpoint)
    server = Server(config, cleanup = true)

    Await.ready(server.start, Duration.Inf)

    server.users.add(TestUsers.hoge)
    server.users.add(TestUsers.s3testsMain)
    server.users.add(TestUsers.s3testsAlt)
  }

  override def afterEach {
    Await.ready(server.stop, Duration.Inf)
  }

  def getTestFile(name: String): File = {
    val loader = getClass.getClassLoader
    new File(loader.getResource(name).getFile)
  }

  def createLargeFile(path: Path, sizeMB: Int): Unit = {
    if (!Files.exists(path)) {
      val result = new Array[Byte](sizeMB * 1024 * 1024)
      new Random().nextBytes(result)
      Files.write(path, result)
    }
  }

  def checkFileContent(actual: S3Object, expected: File) {
    assert(IOUtils.contentEquals(actual.getObjectContent, new FileInputStream(expected)))
  }
}
