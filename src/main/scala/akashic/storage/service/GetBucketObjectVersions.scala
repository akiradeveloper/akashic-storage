package akashic.storage.service

import java.util.Date

import akashic.storage._
import akashic.storage.patch.{Bucket, Version}
import akashic.storage.service.BucketListing.{Group, Single, Filterable}
import akka.http.scaladsl.model.{StatusCodes, HttpRequest}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._

import scala.xml.NodeSeq
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._

object GetBucketObjectVersions {
  val matcher =
    get &
    extractBucket &
    withParameter("uploads") &
    parameters("delimiter"?, "encoding-type"?, "key-marker"?, "max-keys".as[Int]?, "prefix"?, "version-id-marker"?) &
    extractRequest

  val route =
    matcher.as(t)(_.run)

  case class t(bucketName: String,
               delimiter: Option[String],
               encodingType: Option[String],
               keyMarker: Option[String],
               maxKeys: Option[Int],
               prefix: Option[String],
               versionIdMarker: Option[String],
               req: HttpRequest) extends AuthorizedAPI {
    override def name: String = "GET Bucket Object versions"
    override def resource: String = Resource.forBucket(bucketName)
    override def runOnce: Route = {
      sealed trait Xmlable {
        def toXML: NodeSeq
      }
      case class VersionWrap(unwrap: Version) extends Xmlable with Filterable {
        override def name: String = unwrap.key.name
        val meta = unwrap.meta.get
        val acl = unwrap.acl.get
        private def asNormalVersion: NodeSeq =
          <Version>
            <Key>{name}</Key>
            <VersionId>{meta.versionId}</VersionId>
            <IsLatest>true</IsLatest>
            <LastModified>{dates.format000Z(new Date(unwrap.root.getAttr.creationTime))}</LastModified>
            <ETag>{quoteString(meta.eTag)}</ETag>
            <Size>{unwrap.data.length}</Size>
            <Owner>
              <ID>{acl.owner}</ID>
            </Owner>
            <StorageClass>STANDARD</StorageClass>
          </Version>
        // not used at v1.0
        private def asDeleteMarker: NodeSeq =
          <DeleteMarker>
            <Key>sourcekey</Key>
            <VersionId>qDhprLU80sAlCFLu2DWgXAEDgKzWarn-HS_JU0TvYqs.</VersionId>
            <IsLatest>true</IsLatest>
            <LastModified>2009-12-10T16:38:11.000Z</LastModified>
            <Owner>
              <ID>75aa57f09aa0c8caeab4f8c24e99d10f8e7faeebf76c078efc7c6caea54ba06a</ID>
            </Owner>
          </DeleteMarker>
        override def toXML: NodeSeq = {
          meta.isDeleteMarker match {
            case true => asNormalVersion
            case false => asDeleteMarker
          }
        }
      }
      case class CommonPrefixes(members: Seq[VersionWrap], prefix: String) extends Xmlable {
        override def toXML = {
          <CommonPrefixes>
            <Prefix>{prefix}</Prefix>
          </CommonPrefixes>
        }
      }

      val bucket: Bucket = server.tree.findBucket(bucketName) match {
        case Some(a) => a
        case None => failWith(Error.NoSuchBucket())
      }
      val bucketAcl = bucket.acl.get
      if (!bucketAcl.grant(callerId, Acl.Read()))
        failWith(Error.AccessDenied())

      val len = maxKeys match {
        case Some(a) => a
        case None => 1000 // default
      }

      val allVersions: Seq[Single[VersionWrap]] = bucket.listKeys.toSeq
        .sortBy(_.name)
        .map(key => key.versions.listVersions.sortBy(v => v.versionId).map(VersionWrap(_)))
        .flatten
        .map(BucketListing.Single(_))

      val result = allVersions
       .dropWhile(keyMarker.map(km => (a: Single[VersionWrap]) => a.get.unwrap.key.name <= km))
       .dropWhile(versionIdMarker.map(vim => (a: Single[VersionWrap]) => a.get.unwrap.versionId <= vim))
       .filter(prefix.map(pf => (a: Single[VersionWrap]) => a.get.name.startsWith(pf)))
       .groupByDelimiter(delimiter)
       .truncateByMaxLen(len)

      val groups: Seq[Xmlable] = result.value.map {
        case Single(a) => a
        case Group(a, prefix) => CommonPrefixes(a.map(_.get), prefix)
      }

      val resultingXML =
        <ListVersionsResult>
          <Name>{bucketName}</Name>
          { keyMarker.map(a => <KeyMarker>{a}</KeyMarker>).getOrElse(NodeSeq.Empty) }
          { versionIdMarker.map(a => <VersionIdMarker>{a}</VersionIdMarker>).getOrElse(NodeSeq.Empty) }
          { delimiter.map(a => <Delimiter>{a}</Delimiter>).getOrElse(NodeSeq.Empty) }
          { result.nextMarker.map(a => <NextKeyMarker>{a.get.unwrap.key.name}</NextKeyMarker>).getOrElse(NodeSeq.Empty) }
          { result.nextMarker.map(a => <NextVersionIdMarker>{a.get.unwrap.versionId}</NextVersionIdMarker>).getOrElse(NodeSeq.Empty) }
          { maxKeys.map(a => <MaxKeys>{a}</MaxKeys>).getOrElse(NodeSeq.Empty) }
          <IsTruncated>{result.truncated}</IsTruncated>
          { for (group <- groups) yield group.toXML }
        </ListVersionsResult>

      val headers = ResponseHeaderList.builder
        .withHeader(X_AMZ_REQUEST_ID, requestId)
        .build

      complete(StatusCodes.OK, headers, resultingXML)
    }
  }
}
