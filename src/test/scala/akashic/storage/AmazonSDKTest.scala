package akashic.storage

import java.io.{StringBufferInputStream, FileInputStream, File}
import java.nio.file.{Paths, Files, Path}

import akashic.storage.admin.TestUsers
import com.amazonaws.{HttpMethod, ClientConfiguration}
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.model.DeleteObjectsRequest.KeyVersion
import com.amazonaws.services.s3.model._
import com.amazonaws.services.s3.transfer.TransferManager
import com.amazonaws.services.s3.{S3ClientOptions, AmazonS3Client}
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.digest.HmacUtils
import org.apache.commons.io.IOUtils
import org.apache.http.client.methods.{HttpPut, HttpGet, HttpPost}
import org.apache.http.entity.FileEntity
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.impl.client.HttpClients
import org.scalatest.Ignore

import scala.xml.XML

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

  test("add -> delete bucket") { p =>
    import p._
    client.createBucket("myb")
    client.deleteBucket("myb")
    val res = client.listBuckets
    assert(res.size == 0)
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

  test("list objects with / delimiter") { p =>
    import p._
    client.createBucket("myb")
    val f = getTestFile("test.txt")
    client.putObject("myb", "a/b", f)
    client.putObject("myb", "a/c", f)
    val req = new ListObjectsRequest()
      .withBucketName("myb")
      .withDelimiter("/")
    val res = client.listObjects(req)
    assert(res.getCommonPrefixes.size == 1)
    val cp: String = res.getCommonPrefixes.get(0)
    assert(cp == "a")
  }

  test("list versions") { p =>
    import p._
    client.createBucket("myb")

    val f = getTestFile("test.txt")
    client.putObject("myb", "myo2", f)
    client.putObject("myb", "myo1", f)

    val res1 = client.listVersions("myb", "myooooo") // []
    assert(res1.getVersionSummaries.size == 0)

    val res2 = client.listVersions("myb", "myo")
    assert(res2.getVersionSummaries.size === 2) // [myo1, myo2]
    assert(res2.getVersionSummaries.get(0).isDeleteMarker === false)

    val req = new ListVersionsRequest()
      .withBucketName("myb")
      .withDelimiter("o")
    val res3 = client.listVersions(req) // [myo(1,2)]
    assert(res3.getVersionSummaries.size == 0)
    assert(res3.getCommonPrefixes.size == 1)
  }

  test("partial get") { p =>
    import p._

    client.createBucket("a.b")
    val f = getTestFile("test.txt")
    client.putObject("a.b", "myobj.txt", f)

    val get = new GetObjectRequest("a.b", "myobj.txt")
      .withRange(4, 8)

    val obj = client.getObject(get)
    val s = IOUtils.toString(obj.getObjectContent)
    assert(s === "ove S")
  }

  test("conditional get test") { p =>
    import p._
    client.createBucket("a.b")
    val f = getTestFile("test.txt")
    val putRes = client.putObject("a.b", "myobj.txt", f)

    val get = new GetObjectRequest("a.b", "myobj.txt")
      .withNonmatchingETagConstraint(s""""${putRes.getETag}"""")

    val res = client.getObject(get)
    assert(res == null)
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

  test("multiple delete") { p =>
    import p._
    client.createBucket("myb")
    val f = getTestFile("test.txt")
    client.putObject("myb", "myobj1", f)
    client.putObject("myb", "a/b", f)
    client.putObject("myb", "c/d/e", f)
    assert(client.listObjects("myb").getObjectSummaries.size === 3)

    val req = new DeleteObjectsRequest("myb")
    req.setKeys(Seq(new KeyVersion("myobj1"), new KeyVersion("c/d/e")))
    val result = client.deleteObjects(req)
    assert(result.getDeletedObjects.get(1).getKey == "c/d/e")
    assert(result.getDeletedObjects.size === 2)
    assert(result.getDeletedObjects.forall(!_.isDeleteMarker))
    // hmm... the SDK should use Option. returning null is bewildering
    assert(result.getDeletedObjects.forall(_.getDeleteMarkerVersionId === null))

    assert(client.listObjects("myb").getObjectSummaries.size === 1)
    assert(client.listObjects("myb").getObjectSummaries.get(0).getKey === "a/b")
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

  test("presigend get") { p =>
    val cli = p.client

    cli.createBucket("myb")
    val f = getTestFile("test.txt")
    cli.putObject("myb", "a/b", f)

    val expires = new java.util.Date()
    var msec = expires.getTime()
    msec += 1000 * 60 * 60; // 1 hour.
    expires.setTime(msec)

    val generatePresignedUrlRequest = new GeneratePresignedUrlRequest("myb", "a/b")
    generatePresignedUrlRequest.setMethod(HttpMethod.GET)
    generatePresignedUrlRequest.setExpiration(expires)
    val url = cli.generatePresignedUrl(generatePresignedUrlRequest)
    println(url.toString)

    val res = HttpClients.createDefault.execute(new HttpGet(url.toString))
    assert(res.getStatusLine.getStatusCode === 200)
    assert(IOUtils.contentEquals(res.getEntity.getContent, new FileInputStream(f)))
  }

  test("presigned put (or upload)") { p =>
    val cli = p.client
    cli.createBucket("myb")

    val expires = new java.util.Date()
    var msec = expires.getTime()
    msec += 1000 * 60 * 60 // 1 hour.
    expires.setTime(msec)

    val f = getTestFile("test.txt")
    val contentType = "text/plain"

    val req = new GeneratePresignedUrlRequest("myb", "a/b")
    req.setMethod(HttpMethod.PUT)
    req.setExpiration(expires)
    req.setContentType(contentType)
    val url = cli.generatePresignedUrl(req)
    println(url.toString)

    val putReq = new HttpPut(url.toString)
    putReq.setHeader("Content-Type", contentType)
    putReq.setEntity(new FileEntity(f))

    val putRes = HttpClients.createDefault.execute(putReq)
    assert(putRes.getStatusLine.getStatusCode === 200)

    val obj = cli.getObject("myb", "a/b")
    checkFileContent(obj, f)
  }

  test("[performance] put/get XMB") { p =>
    val cli = p.client
    cli.createBucket("myb")
    for (size <- Seq(1, 10, 100)) { // MB
      val filePath = Paths.get(s"/tmp/akashic-storage-test-file-${size}mb")
      createLargeFile(filePath, size)
      val putRes = cli.putObject("myb", "obj1", filePath.toFile)

      val getRes = cli.getObject("myb", "obj1")
    }
  }
}
