package akasha

import akasha.admin.TestUsers
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.{S3ClientOptions, AmazonS3Client}

class AmazonSDKTest extends ServerTestBase {
  case class FixtureParam(client: AmazonS3Client)
  override protected def withFixture(test: OneArgTest) = {
    val conf = new ClientConfiguration
    conf.setSignerOverride("S3SignerType") // force to use v2 signature. not supported in 1.9.7

    val cli = new AmazonS3Client(new BasicAWSCredentials(TestUsers.hoge.accessKey, TestUsers.hoge.secretKey), conf)
    cli.setEndpoint(s"http://${server.config.ip}:${server.config.port}")
    cli.setS3ClientOptions(new S3ClientOptions().withPathStyleAccess(true))

    test(FixtureParam(cli))
  }

  import scala.collection.JavaConversions._

  test("add buckets") { p =>
    val cli = p.client

    cli.createBucket("myb1")
    cli.createBucket("myb2")
    val res = cli.listBuckets
    assert(res.forall(_.getOwner.getId === TestUsers.hoge.id))
    assert(Set(res(0).getName, res(1).getName) === Set("myb1", "myb2"))
    assert(res.size === 2)

    // location
    // val loc = cli.getBucketLocation("mybucket1")
    // assert(loc == "US")
  }
}
