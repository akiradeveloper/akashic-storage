package akashic.storage

import org.scalatest.FunSuite
import akashic.storage.auth._

class AuthTest extends FunSuite {
  val accessKey = "44CF9590006BF252F707"
  val secretKey = "OtxrzxIsfpFjA7SwPzILwy8Bw21TLhquhboDYROV"
  val map1 = Map(accessKey -> secretKey)

  test("example1") {
    assert(V2.doAuthorize(
      "PUT",
      "/quotes/nelson",
      ParamList.empty,
      HeaderList.builder
        .append("Content-Md5", "c8fdb181845a4ca6b8fec737b3581d76")
        .append("Content-Type", "text/html")
        .append("Date", "Thu, 17 Nov 2005 18:49:58 GMT")
        .append("X-Amz-Meta-Author", "foo@bar.com")
        .append("X-Amz-Magic", "abracadabra")
        .append("Authorization", s"AWS ${accessKey}:jZNOcbfWmD/A/f3hSvVzXZjM2HU=")
        .build,
      map1
    ).isDefined)
  }

  test("example2") {
    assert(V2.doAuthorize(
      "GET",
      "/quotes/nelson",
      ParamList.empty,
      HeaderList.builder
        .append("Date", "XXXXXXXXX")
        .append("X-Amz-Magic", "abracadabra")
        .append("X-Amz-Date", "Thu, 17 Nov 2005 18:49:58 GMT")
        .append("Authorization", s"AWS ${accessKey}:5m+HAmc5JsrgyDelh9+a2dNrzN8=")
        .build,
      map1
    ).isDefined)
  }

  test("example3") {
    assert(V2Presigned.doAuthorize(
      "GET",
      "/quotes/nelson",
      ParamList.t(Seq(
        ("AWSAccessKeyId", accessKey),
        ("Expires", "1141889120"),
        ("Signature", "vjbyPxybdZaNmGa%2ByT272YEAiv4%3D")
      )),
      HeaderList.empty,
      map1
    ).isDefined)
  }

  val map2 = Map("myid" -> "mykey")

  test("actual case1: bucket path") {
    assert(V2.doAuthorize(
      "PUT",
      "/mybucket1",
      ParamList.empty,
      HeaderList.builder
        .append("Authorization", "AWS myid:EBrN3wP3EVWxYf3UhxVeBeVFFYI=")
        .append("Date", "Thu, 20 Aug 2015 07:31:47 GMT")
        .append("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
        .build,
      map2  
    ).isDefined)
  }

  test("actual case2: multipart download") {
    assert(V2.doAuthorize(
      "HEAD",
      "/mybucket/myobj",
      ParamList.empty,
      HeaderList.builder
        .append("Authorization", "AWS myid:iYqQ5KhDi0cHGUTeRSFpdVbaQIc=")
        .append("Date", "Tue, 01 Sep 2015 04:11:42 GMT")
        .append("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
        .build,
      map2
    ).isDefined)
  }
}
