package akashic.storage

import akashic.storage.admin.TestUsers
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.{AnonymousAWSCredentials, BasicAWSCredentials}
import com.amazonaws.services.s3.model._
import com.amazonaws.services.s3.{S3ClientOptions, AmazonS3Client}

class AclTest extends ServerTestBase {

  case class FixtureParam(anon: AmazonS3Client,
                          auth1: AmazonS3Client,
                          auth2: AmazonS3Client)
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

    val auth2Conf = new ClientConfiguration()
    auth2Conf.setSignerOverride("S3SignerType")
    val auth2 = new AmazonS3Client(new BasicAWSCredentials(TestUsers.s3testsAlt.accessKey, TestUsers.s3testsAlt.secretKey), auth1Conf)
    auth2.setEndpoint(s"http://${server.address}")
    auth2.setS3ClientOptions(new S3ClientOptions().withPathStyleAccess(true))

    test(FixtureParam(anon, auth1, auth2))
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

  def shouldThrow[A](fn: => A): Unit = {
    intercept[Exception](fn)
  }


  test("anon user can't access to auth resources") { p =>
    import p._
    auth1.createBucket("authb")

    shouldThrow {
      anon.putObject("authb", "anono", FILE)
    }

    auth1.putObject("authb", "autho", FILE)

    shouldThrow {
      anon.getObject("authb", "autho")
    }
  }

  test("auth2 can access auth1 resources (canned acl)") { p =>
    import p._
    auth1.createBucket(new CreateBucketRequest("auth1b")
      .withCannedAcl(CannedAccessControlList.AuthenticatedRead))

    shouldThrow {
      anon.listObjects("auth1b")
    }

    auth2.listObjects("auth1b")
  }

  test("auth2 can't list auth1's buckets") { p =>
    import p._
    anon.createBucket("myba-1")
    auth1.createBucket("myb1-1")
    auth1.createBucket("myb1-2")
    auth2.createBucket("myb2-1")
    assert(auth1.listBuckets.size() === 2)
    assert(auth2.listBuckets.size() === 1)
  }

  test("grant read access by xml") { p =>
    import p._
    auth1.createBucket("myb1")
    val f = getTestFile("test.txt")
    auth1.putObject("myb1", "aaa", f)
    auth1.putObject("myb1", "bbb", f)
    shouldThrow(auth2.listObjects("myb1"))
    val acl = new AccessControlList()
    val auth2User = TestUsers.s3testsAlt
    acl.grantPermission(new CanonicalGrantee(auth2User.id), Permission.Read)
    acl.setOwner(new Owner(auth2User.id, auth2User.displayName))
    auth1.setBucketAcl("myb1", acl)
    auth2.listObjects("myb1")
    shouldThrow(auth2.putObject("myb1", "ccc", f))
    // auth1.listObjects("myb1")
  }
}
