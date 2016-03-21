package akashic.storage

import akashic.storage.admin.TestUsers
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AnonymousAWSCredentials
import com.amazonaws.services.s3.{S3ClientOptions, AmazonS3Client}
import org.scalatest.Outcome
import scala.collection.JavaConversions._

class AnonymousTest extends ServerTestBase {
  case class FixtureParam(cli: AmazonS3Client)
  override protected def withFixture(test: OneArgTest): Outcome = {
    val anonConf = new ClientConfiguration
    val anon = new AmazonS3Client(new AnonymousAWSCredentials(), anonConf)
    anon.setEndpoint(s"http://${server.address}")
    anon.setS3ClientOptions(new S3ClientOptions().withPathStyleAccess(true))

    test(FixtureParam(anon))
  }

  test("ls service") { p =>
    import p._

    cli.createBucket("myb1")
    cli.createBucket("myb2")

    val res = cli.listBuckets
    assert(res.forall(_.getOwner.getId === "anonymous"))
    assert(Set(res(0).getName, res(1).getName) === Set("myb1", "myb2"))
    assert(res.size === 2)
  }

  test("ls bucket") { p =>
    import p._

    cli.createBucket("myb")
    val f = getTestFile("test.txt")
    cli.putObject("myb", "myobj1", f)
    cli.putObject("myb", "myobj2", f)
    val objListing = cli.listObjects("myb")
    assert(objListing.getBucketName == "myb")
    val summaries = objListing.getObjectSummaries
    assert(summaries.size === 2)
    assert(summaries(0).getOwner.getId === "anonymous")
  }
}
