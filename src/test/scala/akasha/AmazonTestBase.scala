package akasha

import akasha.admin.TestUsers
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.{S3ClientOptions, AmazonS3Client}

abstract class AmazonTestBase extends ServerTestBase {
  case class FixtureParam(client: AmazonS3Client)
  override protected def withFixture(test: OneArgTest) = {
    val conf = new ClientConfiguration
    conf.setSignerOverride("S3SignerType") // force to use v2 signature. not supported in 1.9.7

    val cli = new AmazonS3Client(new BasicAWSCredentials(TestUsers.hoge.accessKey, TestUsers.hoge.secretKey), conf)
    cli.setEndpoint(s"http://${server.config.ip}:${server.config.port}")
    cli.setS3ClientOptions(new S3ClientOptions().withPathStyleAccess(true))

    test(FixtureParam(cli))
  }
}
