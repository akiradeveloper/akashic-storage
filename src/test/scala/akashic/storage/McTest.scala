package akashic.storage

import java.io.File
import java.nio.file.{Paths, Files}

import akashic.storage.server
import akashic.storage.admin.User
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.{IOUtils, FileUtils}
import org.apache.http.client.methods.{HttpGet, HttpPost}
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicHeader
import scala.xml.XML
import scala.sys.process.Process

class McTest extends ServerTestBase {
  case class FixtureParam()
  override protected def withFixture(test: OneArgTest) = {
    test(FixtureParam())
  }

  val alias = "akashic-storage"
  val confDir = "/tmp/akashic-storage-mc"

  def mc(cmd: String) = {
    // val cmdline = s"mc --config-folder=${confDir} ${cmd}"
    val cmdline = s"mc --debug ${cmd}"
    println(cmdline)
    Process(cmdline)
  }

  override def beforeEach() {
    super.beforeEach()

    val url = s"http://${server.address}/admin/user"
    val authHeader = new BasicHeader("Authorization", s"Basic ${Base64.encodeBase64URLSafeString("admin:passwd".getBytes)}")

    val postReq = new HttpPost(url)
    postReq.addHeader(authHeader)

    val postRes = HttpClients.createDefault.execute(postReq)
    assert(postRes.getStatusLine.getStatusCode === 200)
    val newUser = User.fromXML(XML.load(postRes.getEntity.getContent))
    println(newUser)

    val confDirPath = Paths.get(confDir)
    if (!Files.exists(confDirPath)) {
      Files.createDirectory(confDirPath)
    }
    FileUtils.cleanDirectory(confDirPath.toFile)

    assert(mc(s"config host add ${alias} http://${server.address} ${newUser.accessKey} ${newUser.secretKey} S3v2").! === 0)
    // assert(mc(s"config alias add ${alias} http://${server.address}").! === 0)
  }

  test("add buckets") { _ =>
    assert(mc(s"ls ${alias}").!!.split("\n").length === 1)
    assert(mc(s"mb ${alias}/abc").! === 0)
    assert(mc(s"ls ${alias}").!!.split("\n").length === 1)
    assert(mc(s"mb ${alias}/def").! === 0)
    assert(mc(s"ls ${alias}").!!.split("\n").length === 2)
  }

  test("ls bucket") { _ =>
    mc(s"mb ${alias}/abc").!
    val f = getTestFile("test.txt")
    // in sbt test we need to add --quiet flag otherwise
    // mc: <ERROR> Unable to get terminal size. Please use --quiet option. inappropriate ioctl for device
    mc(s"--quiet cp ${f.getAbsolutePath} ${alias}/abc/aaaaa").!
    mc(s"--quiet cp ${f.getAbsolutePath} ${alias}/abc/bbbbb").!
    mc(s"ls ${alias}/abc/").!
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

  test("share (presigned get)") { _ =>
    assert(mc(s"mb ${alias}/abc").! === 0)
    val f = getTestFile("test.txt")
    assert(mc(s"--quiet cp ${f.getAbsolutePath} ${alias}/abc").! === 0)

    val url = (mc(s"share download ${alias}/abc/test.txt").!!).split("\n")(2).split("Share: ")(1)
    println(url)
    val res = HttpClients.createDefault.execute(new HttpGet(url))
    assert(IOUtils.toString(res.getEntity.getContent) === "We love Scala!")
  }

  test("preproduce daemon-test") { _ =>
    (Process("echo aaaaaa") #> new File("/tmp/file-up")).!
    assert(mc(s"mb ${alias}/myb").! === 0)
    assert(mc(s"--quiet cp /tmp/file-up ${alias}/myb/myo").! === 0)
    assert(mc(s"--quiet cat ${alias}/myb/myo").! === 0)
  }
}
