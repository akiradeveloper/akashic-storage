package akashic.storage

import java.nio.file.{Paths, Files}

import akashic.storage.server
import akashic.storage.admin.User
import org.apache.commons.io.FileUtils
import scala.xml.XML
import scalaj.http.Http
import scala.sys.process.Process

class McTest extends ServerTestBase {
  case class FixtureParam()
  override protected def withFixture(test: OneArgTest) = {
    test(FixtureParam())
  }

  val alias = "akashic-storage"
  val confDir = "/tmp/akashic-storage-mc"

  def mc(cmd: String) = {
    val cmdline = s"mc --config-folder=${confDir} ${cmd}"
    println(cmdline)
    Process(cmdline)
  }

  override def beforeEach() {
    super.beforeEach()

    val url = s"http://${server.address}/admin/user"
    val postRes = Http(url).method("POST").asString
    assert(postRes.code === 200)
    val newUser = User.fromXML(XML.loadString(postRes.body))
    println(newUser)

    val confDirPath = Paths.get(confDir)
    if (!Files.exists(confDirPath)) {
      Files.createDirectory(confDirPath)
    }
    FileUtils.cleanDirectory(confDirPath.toFile)

    assert(mc(s"config host add http://${server.address} ${newUser.accessKey} ${newUser.secretKey} S3v2").! === 0)
    assert(mc(s"config alias add ${alias} http://${server.address}").! === 0)
  }

  test("add buckets") { _ =>
    assert(mc(s"ls ${alias}").!!.split("\n").length === 1)
    assert(mc(s"mb ${alias}/abc").! === 0)
    assert(mc(s"ls ${alias}").!!.split("\n").length === 1)
    assert(mc(s"mb ${alias}/DEF").! === 0)
    assert(mc(s"ls ${alias}").!!.split("\n").length === 2)
  }

  test("cp file and cat") { _ =>
    assert(mc(s"mb ${alias}/abc").! === 0)

    val f = getTestFile("test.txt")

    // by dir dest
    assert(mc(s"--quiet cp ${f.getAbsolutePath} ${alias}/abc").! === 0)
    assert(mc(s"cat ${alias}/abc/test.txt").!!.trim === "We love Scala!")

    // by explicit dest name
    assert(mc(s"--quiet cp ${f.getAbsolutePath} ${alias}/abc/test2.txt").! === 0)
    assert(mc(s"cat ${alias}/abc/test2.txt").!!.trim === "We love Scala!")
  }

  test("pipe and cat") { _ =>
    assert(mc(s"mb ${alias}/abc").! === 0)
    assert((Process("echo hoge") #| mc(s"pipe ${alias}/abc/hoge.txt")).! === 0)
    assert(mc(s"cat ${alias}/abc/hoge.txt").!!.trim === "hoge")
  }

  // test("share (presigned get)") { _ =>
  //   (mc(s"mb ${alias}/abc") !) orFail
  //   val f = getTestFile("test.txt")
  //   (mc(s"--quiet cp ${f.getAbsolutePath} ${alias}/abc") !) orFail
  //
  //   val httpCli = HttpClients.createDefault
  //   val url: String = (mc(s"share ${alias}/abc/test.txt") !!).split("\n")(1).split("URL: ")(1)
  //
  //   val method = new HttpGet(url)
  //   val contents: String = IOUtils.toString(httpCli.execute(method).getEntity.getContent).trim
  //   assert(contents === "We love Scala!")
  // }
}
