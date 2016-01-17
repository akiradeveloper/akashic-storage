package akashic.storage.service

import akashic.storage.{files, Server}
import akashic.storage.service.Error.Reportable
import io.finch._
import akashic.storage.patch.Part

import scala.xml.NodeSeq

trait ListPartsSupport {
  self: Server =>
  object ListParts {
    val matcher = get(string / string / paramExists("uploadId") ?
      param("uploadId") ?
      paramOption("part-number-marker").as[Int] ?
      paramOption("max-parts").as[Int] ?
      RequestId.reader ?
      CallerId.reader).as[t]
    val endpoint = matcher { a: t => a.run }
    case class t(bucketName: String, keyName: String,
                 uploadId: String,
                 partNumberMarker: Option[Int],
                 maxParts: Option[Int],
                 requestId: String, callerId: String) extends Task[Output[NodeSeq]] with Reportable {
      def name = "List Parts"
      def resource = Resource.forObject(bucketName, keyName)
      def runOnce = {
        val bucket = findBucket(tree, bucketName)
        val key = findKey(bucket, keyName)
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
          .filter(_.find.isDefined) // having valid upload

        val truncated = emitList0.size > listMaxLen

        val emitList: Seq[Part] = emitList0.take(listMaxLen)

        val nextPartNumberMarker = emitList.lastOption match {
          case Some(a) => a.id
          case None => 0
        }

        val acl = Acl.fromBytes(upload.acl.readBytes)

        val ownerId = acl.owner

        // If the initiator is an AWS account, this element provides the same information as the Owner element.
        // If the initiator is an IAM User, then this element provides the user ARN and display name.
        val initiatorId = ownerId

        def xmlPart(part: Part): NodeSeq = {
          val filePath = part.find.get.filePath
          <Part>
            <PartNumber>{part.id}</PartNumber>
            <LastModified>{dates.format000Z(files.lastDate(filePath))}</LastModified>
            <ETag>{files.computeMD5(part.find.get.filePath)}</ETag>
            <Size>{files.fileSize(filePath)}</Size>
          </Part>
        }

        val xml =
          <ListPartsResult>
            <Bucket>{bucketName}</Bucket>
            <Key>{keyName}</Key>
            <UploadId>{uploadId}</UploadId>
            <Initiator>
              <ID>{initiatorId}</ID>
              <DisplayName>{users.getUser(initiatorId).get.displayName}</DisplayName>
            </Initiator>
            <Owner>
              <ID>{ownerId}</ID>
              <DisplayName>{users.getUser(ownerId).get.displayName}</DisplayName>
            </Owner>
            <StorageClass>STANDARD</StorageClass>
            { partNumberMarker match { case Some(a) => <PartNumberMarker>{a}</PartNumberMarker>; case None => NodeSeq.Empty } }
            <NextPartNumberMarker>{nextPartNumberMarker}</NextPartNumberMarker>
            { maxParts match { case Some(a) => <MaxParts>{a}</MaxParts>; case None => NodeSeq.Empty } }
            <IsTruncated>{truncated}</IsTruncated>
            { for (part <- emitList) yield xmlPart(part) }
          </ListPartsResult>

        Ok(xml)
          .withHeader(X_AMZ_REQUEST_ID -> requestId)
      }
    }
  }
}
