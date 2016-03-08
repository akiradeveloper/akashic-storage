package akashic.storage

import akashic.storage.admin.TestUsers
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.{AnonymousAWSCredentials, BasicAWSCredentials}
import com.amazonaws.services.s3.{S3ClientOptions, AmazonS3Client}

class AclTest extends ServerTestBase {

  case class FixtureParam(anon: AmazonS3Client, auth1: AmazonS3Client)
  override protected def withFixture(test: OneArgTest) = {
    val anonConf = new ClientConfiguration
    val anon = new AmazonS3Client(new AnonymousAWSCredentials(), anonConf)
    anon.setEndpoint(s"http://${server.address}")
    anon.setS3ClientOptions(new S3ClientOptions().withPathStyleAccess(true))

    val auth1Conf = new ClientConfiguration()
    auth1Conf.setSignerOverride("S3SignerType")
    val auth1 = new AmazonS3Client(new BasicAWSCredentials(TestUsers.s3testsMain.accessKey, TestUsers.s3testsMain.secretKey), auth1Conf)
    auth1.setEndpoint(s"http://${server.address}")
    auth1.setS3ClientOptions(new S3ClientOptions().withPathStyleAccess(true))

    test(FixtureParam(anon, auth1))
  }

  val FILE = getTestFile("test.txt")

  test("make a bucket and put") { p =>
    import p._
    anon.createBucket("myb")
    anon.putObject("myb", "obj1", FILE)
  }

  test("[auth1] make a bucket and put") { p =>
    import p._
    auth1.createBucket("myb")
    auth1.putObject("myb", "obj1", FILE)
  }

  test("auth user can access anon resources") { p =>
    import p._
    anon.createBucket("anonb")
    auth1.putObject("anonb", "autho", FILE)

    anon.putObject("anonb", "anono", FILE)
    auth1.getObject("anonb", "anono")
  }

  test("anon user can't access to auth resources") { p =>
    import p._
    auth1.createBucket("authb")
    anon.putObject("authb", "anono", FILE) // should throw

    auth1.putObject("authb", "autho", FILE)
    anon.getObject("authb", "autho") // should throw
  }
}
