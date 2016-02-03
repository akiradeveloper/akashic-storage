package akashic.storage

import java.io.{FileInputStream, File}
import java.nio.file.{Paths, Files, Path}

import akashic.storage.admin.TestUsers
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.model._
import com.amazonaws.services.s3.transfer.TransferManager
import com.amazonaws.services.s3.{S3ClientOptions, AmazonS3Client}
import org.apache.commons.io.IOUtils

import scalaj.http.Http

class AmazonSDKTest extends ServerTestBase {
  case class FixtureParam(client: AmazonS3Client)
  override protected def withFixture(test: OneArgTest) = {
    val conf = new ClientConfiguration
    conf.setSignerOverride("S3SignerType") // force to use v2 signature. not supported in 1.9.7

    val cli = new AmazonS3Client(new BasicAWSCredentials(TestUsers.hoge.accessKey, TestUsers.hoge.secretKey), conf)
    cli.setEndpoint(s"http://${server.address}")
    cli.setS3ClientOptions(new S3ClientOptions().withPathStyleAccess(true))

    test(FixtureParam(cli))
  }

  import scala.collection.JavaConversions._

  test("no buckets") { p =>
    import p._
    val res = client.listBuckets
    assert(res.size == 0)
  }

  test("add buckets") { p =>
    import p._

    client.createBucket("myb1")

    client.createBucket("myb2")
    val res = client.listBuckets
    assert(res.forall(_.getOwner.getId === TestUsers.hoge.id))
    assert(Set(res(0).getName, res(1).getName) === Set("myb1", "myb2"))
    assert(res.size === 2)

    // location
    // val loc = cli.getBucketLocation("mybucket1")
    // assert(loc == "US")
  }

  test("put and get object") { p =>
    import p._

    client.createBucket("a.b")
    val f = getTestFile("test.txt")
    val putRes = client.putObject("a.b", "myobj.txt", f)
    assert(putRes.getVersionId === "null")

    val obj1 = client.getObject("a.b", "myobj.txt")
    checkFileContent(obj1, f)

    // test second read
    val obj2 = client.getObject("a.b", "myobj.txt")
    checkFileContent(obj2, f)
  }

  test("put key delimited") { p =>
    import p._
    client.createBucket("myb")
    val f = getTestFile("test.txt")
    client.putObject("myb", "a/b", f)
    val obj = client.getObject("myb", "a/b")
    checkFileContent(obj, f)
  }

  test("put and get image object") { p =>
    import p._

    client.createBucket("myb")
    val f = getTestFile("test.jpg")
    client.putObject("myb", "myobj", f)
    val obj = client.getObject("myb", "myobj")
    checkFileContent(obj, f)
  }

  test("put 8mb file") { p =>
    import p._
    client.createBucket("myb")
    val path = Paths.get("/tmp/akashic-storage-test-file-8mb")
    createLargeFile(path, 8)
    val f = path.toFile
    client.putObject("myb", "myobj", f)
    val obj = client.getObject("myb", "myobj")
    checkFileContent(obj, f)
  }

  test("put and get several objects") { p =>
    import p._

    client.createBucket("myb")
    val f = getTestFile("test.txt")
    client.putObject("myb", "myobj1", f)
    client.putObject("myb", "myobj2", f)
    val objListing = client.listObjects("myb")
    assert(objListing.getBucketName == "myb")
    val summaries = objListing.getObjectSummaries
    assert(summaries.size === 2)
    assert(summaries(0).getOwner.getId === TestUsers.hoge.id)

    val obj1 = client.getObject("myb", "myobj1")
    checkFileContent(obj1, f)
    val obj2 = client.getObject("myb", "myobj2")
    checkFileContent(obj2, f)
  }

  test("overwrite") { p =>
    import p._
    client.createBucket("myb")
    val bucket = server.tree.findBucket("myb").get

    val f1 = getTestFile("test.txt")
    client.putObject("myb", "myobj", f1)
    val key = bucket.findKey("myobj").get
    val obj1 = client.getObject("myb", "myobj")
    checkFileContent(obj1, f1)

    val f2 = getTestFile("test.jpg")
    client.putObject("myb", "myobj", f2)
    val obj2 = client.getObject("myb", "myobj")
    checkFileContent(obj2, f2)
  }

  test("delete an object") { p =>
    import p._

    client.createBucket("myb")
    val f = getTestFile("test.txt")

    client.putObject("myb", "myobj1", f)
    client.putObject("myb", "myobj2", f)
    assert(client.listObjects("myb").getObjectSummaries.size === 2)

    client.deleteObject("myb", "myobj1")
    assert(client.listObjects("myb").getObjectSummaries.size === 1)
    assert(client.listObjects("myb").getObjectSummaries.get(0).getKey === "myobj2")
  }

  test("put -> delete -> put") { p =>
    import p._

    client.createBucket("myb")
    val f = getTestFile("test.txt")
    assert(client.listObjects("myb").getObjectSummaries.size === 0)
    client.putObject("myb", "a", f)
    assert(client.listObjects("myb").getObjectSummaries.size === 1)
    client.deleteObject("myb", "a")
    assert(client.listObjects("myb").getObjectSummaries.size === 0)
    client.putObject("myb", "a", f)
    assert(client.listObjects("myb").getObjectSummaries.size === 1)
  }

  test("multipart upload (lowlevel)") { p =>
    import p._

    client.createBucket("myb")

    val filePath = Paths.get("/tmp/akashic-storage-test-file-32mb")
    createLargeFile(filePath, 32)
    val upFile = filePath.toFile

    val initReq = new InitiateMultipartUploadRequest("myb", "myobj")
    val initRes = client.initiateMultipartUpload(initReq)
    assert(initRes.getBucketName === "myb")
    assert(initRes.getKey === "myobj")
    assert(initRes.getUploadId.length > 0)

    import scala.collection.mutable
    val partEtags = mutable.ListBuffer[PartETag]()
    val contentLength = upFile.length

    var i = 1
    var filePos: Long = 0
    while (filePos < contentLength) {
      val partSize = Math.min(contentLength - filePos, 5 * 1024 * 1024)

      val uploadReq = new UploadPartRequest()
        .withBucketName("myb")
        .withKey("myobj")
        .withUploadId(initRes.getUploadId())
        .withPartNumber(i)
        .withFileOffset(filePos)
        .withFile(upFile)
        .withPartSize(partSize)

      val res = client.uploadPart(uploadReq)
      assert(res.getETag.size > 0)
      assert(res.getPartNumber === i)
      assert(res.getPartETag.getETag.size > 0)
      assert(res.getPartETag.getPartNumber === i)

      partEtags.add(res.getPartETag)

      i += 1
      filePos += partSize
    }

    val compReq = new CompleteMultipartUploadRequest(
      "myb",
      "myobj",
      initRes.getUploadId(),
      partEtags
    )
    val compRes = client.completeMultipartUpload(compReq)
    assert(compRes.getETag.size > 0)
    assert(compRes.getBucketName === "myb")
    assert(compRes.getKey === "myobj")
    assert(compRes.getVersionId === "null")
    assert(compRes.getLocation === s"http://${server.address}/myb/myobj")

    val obj = client.getObject("myb", "myobj")
    checkFileContent(obj, upFile)
  }

  test("multipart upload (highlevel)") { p =>
    import p._

    client.createBucket("myb")

    val filePath = Paths.get("/tmp/akashic-storage-test-file-32mb")
    createLargeFile(filePath, 32)
    val upFile = filePath.toFile

    val tmUp = new TransferManager(client)
    val upload = tmUp.upload("myb", "myobj", upFile)
    upload.waitForCompletion()
    tmUp.shutdownNow(false) // shutdown s3 client = false

    val obj = client.getObject("myb", "myobj")
    checkFileContent(obj, upFile)

    val tmDown = new TransferManager(client)
    val downFile = Paths.get("/tmp/akashic-storage-test-large-file-download").toFile
    val download = tmDown.download(new GetObjectRequest("myb", "myobj"), downFile)
    download.waitForCompletion()
    tmDown.shutdownNow(false)

    assert(IOUtils.contentEquals(
      Files.newInputStream(upFile.toPath),
      Files.newInputStream(downFile.toPath)
    ))
  }
}
