package akashic.storage.service

import akashic.storage._
import akashic.storage.auth.{GetCallerId, V2Post}
import akashic.storage.service.Acl.CannedAcl
import akka.http.scaladsl.model.{StatusCode, HttpEntity}
import akka.http.scaladsl.model.headers.ETag
import akka.http.scaladsl.server.Directives._
import scala.xml.NodeSeq
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._

@deprecated
object PostObject {
  val matcher =
    post &
      extractBucket &
      formFields( // formFields is case-sensitive
        "key",
        "file".as[Array[Byte]],
        "acl"?,
        "success_action_status".as[Int]?,
        "Content-Type"?,
        "Content-Disposition"?,
        "Policy"?,
        "AWSAccessKeyId"?,
        "Signature"?
      ) &
      extractMetadataFromFields

  val route = matcher.as(t)(_.run)

  case class t(bucketName: String, keyNameSlashed: String,
               data: Array[Byte],
               acl: Option[String],
               successActionStatus: Option[Int],
               contentType: Option[String],
               contentDisposition: Option[String],
               policy: Option[String],
               accessKey: Option[String],
               signature: Option[String],
               metadata: HeaderList.t) extends AnonymousAPI {
    override def name = "POST Object"
    override def resource = Resource.forBucket(bucketName)
    override def runOnce = {
      val authKey: Option[String] = V2Post(policy, accessKey, signature).authorize
      val callerId = GetCallerId(authKey, requestId, resource).run

      val keyName = encodeKeyName(keyNameSlashed)
      val result = MakeObject.t(bucketName, keyName, data, acl, Seq.empty, contentType, contentDisposition,
        None, // contentMd5 isn't allowed to POST Object
        metadata, callerId, requestId).run
      val headers = ResponseHeaderList.builder
        .withHeader(X_AMZ_REQUEST_ID, requestId)
        .withHeader(X_AMZ_VERSION_ID, result.versionId)
        .withHeader(ETag(result.etag))
        .build

      val xml =
        <PostResponse>
          <Bucket>{bucketName}</Bucket>
          <Key>{keyNameSlashed}</Key>
          <ETag>{result.etag}</ETag>
          <Location>{s"http://${server.address}/${bucketName}/${keyName}"}</Location>
        </PostResponse>

      // If the value is set to 200 or 204, Amazon S3 returns an empty document with a 200 or 204 status code.
      // If the value is set to 201, Amazon S3 returns an XML document with a 201 status code.
      // If the value is not set or if it is set to an invalid value, Amazon S3 returns an empty document with a 204 status code.
      val retCode: Int = successActionStatus.getOrElse(204)
      retCode match {
        case 200 | 204 => complete(retCode, headers, HttpEntity.Empty)
        case 201 => complete(retCode, headers, xml)
      }
    }
  }
}
