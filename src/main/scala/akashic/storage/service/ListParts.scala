package akashic.storage.service

import java.util.Date

import akashic.storage.patch.Part
import akashic.storage.server
import akka.http.scaladsl.model.{StatusCodes, HttpRequest}
import akka.http.scaladsl.server.Directives._

import scala.xml.NodeSeq
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._

object ListParts {
  val matcher =
    get &
    extractObject &
    parameters("uploadId", "part-number-marker".as[Int]?, "max-parts".as[Int]?)

  val route = matcher.as(t)(_.run)

  case class t(bucketName: String, keyName: String,
               uploadId: String,
               partNumberMarker: Option[Int],
               maxParts: Option[Int]) extends AuthorizedAPI {
    def name = "List Parts"
    def resource = Resource.forObject(bucketName, keyName)
    def runOnce = {
      val bucket = findBucket(server.tree, bucketName)
      val key = findKey(bucket, keyName, Error.NoSuchUpload())
      val upload = findUpload(key, uploadId)

      val startNumber = partNumberMarker match {
        case Some(a) => a
        case None => 0
      }

      val listMaxLen = maxParts match {
        case Some(a) => a
        case None => 1000
      }

      val emitList0 = upload.listParts
        .dropWhile (_.id < startNumber)

      val truncated = emitList0.size > listMaxLen

      val emitList: Seq[Part] = emitList0.take(listMaxLen)

      val nextPartNumberMarker = emitList.lastOption match {
        case Some(a) => a.id
        case None => 0
      }

      val acl = upload.acl.get

      val ownerId = acl.owner

      // If the initiator is an AWS account, this element provides the same information as the Owner element.
      // If the initiator is an IAM User, then this element provides the user ARN and display name.
      val initiatorId = ownerId

      def xmlPart(part: Part): NodeSeq = {
        val filePath = part.unwrap.filePath
        <Part>
          <PartNumber>{part.id}</PartNumber>
          <LastModified>{dates.format000Z(new Date(filePath.getAttr.creationTime))}</LastModified>
          <ETag>{filePath.computeMD5}</ETag>
          <Size>{filePath.getAttr.length}</Size>
        </Part>
      }

      val xml =
        <ListPartsResult>
          <Bucket>{bucketName}</Bucket>
          <Key>{keyName}</Key>
          <UploadId>{uploadId}</UploadId>
          <Initiator>
            <ID>{initiatorId}</ID>
            <DisplayName>{server.users.find(initiatorId).get.displayName}</DisplayName>
          </Initiator>
          <Owner>
            <ID>{ownerId}</ID>
            <DisplayName>{server.users.find(ownerId).get.displayName}</DisplayName>
          </Owner>
          <StorageClass>STANDARD</StorageClass>
          { partNumberMarker match { case Some(a) => <PartNumberMarker>{a}</PartNumberMarker>; case None => NodeSeq.Empty } }
          <NextPartNumberMarker>{nextPartNumberMarker}</NextPartNumberMarker>
          { maxParts match { case Some(a) => <MaxParts>{a}</MaxParts>; case None => NodeSeq.Empty } }
          <IsTruncated>{truncated}</IsTruncated>
          { for (part <- emitList) yield xmlPart(part) }
        </ListPartsResult>


      val headers = ResponseHeaderList.builder
        .withHeader(X_AMZ_REQUEST_ID, requestId)
        .build

      complete(StatusCodes.OK, headers, xml)
    }
  }
}
