package akashic.storage.service

import java.util.Date

import akashic.storage.patch.{Bucket, Upload}
import akashic.storage.server
import akashic.storage.service.BucketListing.{Group, Single}
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import scala.xml.NodeSeq

object ListMultipartUploads {

  val matcher =
    get &
    extractBucket &
    withParameter("uploads") &
    parameters("delimiter"?, "encoding-type"?, "key-marker"?, "upload-id-marker"?, "max-uploads".as[Int]?, "prefix"?)

  val route = matcher.as(t)(_.run)

  case class t(bucketName: String,
               delimiter: Option[String],
               encodingType: Option[String],
               keyMarker: Option[String],
               uploadIdMarker: Option[String],
               maxUploads: Option[Int],
               prefix: Option[String]) extends AuthorizedAPI {
    override def name: String = "List Multipart Uploads"

    override def resource: String = Resource.forBucket(bucketName)

    sealed trait Xmlable {
      def toXML: NodeSeq
    }
    case class UploadWrap(keyName: String, upload: Upload) extends Xmlable with BucketListing.Filterable {
      override def name: String = keyName
      override def toXML: NodeSeq = {
        val acl = upload.acl.get
        val ownerId = acl.owner
        val displayName = server.users.find(ownerId).get.displayName
        val initiatedDate = dates.format000Z(new Date(upload.root.getAttr.creationTime))
        <Upload>
          <Key>{keyName}</Key>
          <UploadId>{upload.name}</UploadId>
          <Initiator>
            <ID>{ownerId}</ID>
            <DisplayName>{displayName}</DisplayName>
          </Initiator>
          <Owner>
            <ID>{ownerId}</ID>
            <DisplayName>{displayName}</DisplayName>
          </Owner>
          <StorageClass>STANDARD</StorageClass>
          <Initiated>{initiatedDate}</Initiated>
        </Upload>
      }
    }
    case class CommonPrefixes(uploads: Seq[UploadWrap], prefix: String) extends Xmlable {
      override def toXML: NodeSeq = {
        <CommonPrefixes>
          <Prefix>{prefix}</Prefix>
        </CommonPrefixes>
      }
    }

    override def runOnce: Route = {
      val bucket: Bucket = server.tree.findBucket(bucketName) match {
        case Some(a) => a
        case None => failWith(Error.NoSuchBucket())
      }
      val bucketAcl = bucket.acl.get
      if (!bucketAcl.grant(callerId, Acl.Read()))
        failWith(Error.AccessDenied())

      val len = maxUploads match {
        case Some(a) => a
        case None => 1000 // default
      }

      val allUploads: Seq[Single[UploadWrap]] = bucket.listKeys.toSeq
        .sortBy(_.name)
        .map(key => key.uploads.listUploads.toSeq.sortBy(_.name).map(UploadWrap(key.name, _)))
        .flatten
        .map(BucketListing.Single(_))

      val result =
        allUploads
        .dropWhile(keyMarker.map(km => (a: Single[UploadWrap]) => a.get.keyName <= km))
        .dropWhile(uploadIdMarker.map(uim => (a: Single[UploadWrap]) => a.get.upload.name <= uim))
        .filter(prefix.map(pf => (a: Single[UploadWrap]) => a.get.name.startsWith(pf)))
        .groupByDelimiter(delimiter)
        .truncateByMaxLen(len)

      val groups: Seq[Xmlable] = result.value.map {
        case Single(a) => a
        case Group(a, prefix) => CommonPrefixes(a.map(_.get), prefix)
      }

      val resultingXML =
        <ListMultipartUploadsResult>
          <Bucket>${bucketName}</Bucket>
          { keyMarker.map(a => <KeyMarker>{a}</KeyMarker>).getOrElse(NodeSeq.Empty) }
          { uploadIdMarker.map(a => <UploadIdMarker>{a}</UploadIdMarker>).getOrElse(NodeSeq.Empty) }
          { delimiter.map(a => <Delimiter>{a}</Delimiter>).getOrElse(NodeSeq.Empty) }
          { result.nextMarker.map(a => <NextKeyMarker>{decodeKeyName(a.get.keyName)}</NextKeyMarker>).getOrElse(NodeSeq.Empty) }
          { result.nextMarker.map(a => <NextUploadIdMarker>{a.get.upload.name}</NextUploadIdMarker>).getOrElse(NodeSeq.Empty) }
          { maxUploads.map(a => <MaxUploads>{a}</MaxUploads>).getOrElse(NodeSeq.Empty) }
          <IsTruncated>{result.truncated}</IsTruncated>
          { for (group <- groups) yield group.toXML }
        </ListMultipartUploadsResult>

      val headers = ResponseHeaderList.builder
        .withHeader(X_AMZ_REQUEST_ID, requestId)
        .build

      complete(StatusCodes.OK, headers, resultingXML)
    }
  }
}
