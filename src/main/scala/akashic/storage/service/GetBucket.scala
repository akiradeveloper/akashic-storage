package akashic.storage.service

import java.util.Date

import akashic.storage.patch.Version
import akka.http.scaladsl.model.{StatusCodes, HttpRequest}
import akka.http.scaladsl.server.Directives._
import akashic.storage.server
import akka.http.scaladsl.server.Route
import scala.xml.NodeSeq
import scala.util.Try
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._

object GetBucket {
  val matcher =
    get &
    extractBucket &
    parameters("delimiter"?, "encoding-type"?, "marker"?, "max-keys".as[Int]?, "prefix"?) &
    extractRequest
  val route = matcher.as(t)(_.run)
  case class t(bucketName: String,
               delimiter: Option[String],
               encodingType: Option[String],
               marker: Option[String],
               maxKeys: Option[Int],
               prefix: Option[String],
               req: HttpRequest) extends AuthorizedAPI {
    def name = "GET Bucket"
    def resource = Resource.forBucket(bucketName)
    def runOnce = {
      sealed trait Xmlable {
        def toXML: NodeSeq
      }
      case class Contents(version: Version) extends Xmlable with BucketListing.Filterable {
        val acl = version.acl.get
        val meta = version.meta.get
        val ownerId = acl.owner
        val key = version.key
        override def toXML = {
          <Contents>
            <Key>{decodeKeyName(key.name)}</Key>
            <LastModified>{dates.format000Z(new Date(version.root.getAttr.creationTime))}</LastModified>
            <ETag>{quoteString(meta.eTag)}</ETag>
            <Size>{version.data.length}</Size>
            <StorageClass>STANDARD</StorageClass>
            <Owner>
              <ID>{ownerId}</ID>
              <DisplayName>{server.users.find(ownerId).get.displayName}</DisplayName>
            </Owner>
          </Contents>
        }
        override def name: String = key.name
      }
      case class CommonPrefixes(members: Seq[Contents], prefix: String) extends Xmlable {
        override def toXML = {
          <CommonPrefixes>
            <Prefix>{prefix}</Prefix>
          </CommonPrefixes>
        }
      }

      val bucket = server.tree.findBucket(bucketName) match {
        case Some(a) => a
        case None => failWith(Error.NoSuchBucket())
      }
      val bucketAcl = bucket.acl.get
      if (!bucketAcl.grant(callerId, Acl.Read()))
        failWith(Error.AccessDenied())

      import BucketListing._
      val len = maxKeys match {
        case Some(a) => a
        case None => 1000 // dafault
      }

      val allContents = bucket.listKeys
        .map(_.findLatestVersion)
        .filter(_.isDefined).map(_.get) // List[Version]
        .filter { version =>
          val meta = version.meta.get
          !meta.isDeleteMarker
        }
        .toSeq.sortBy(_.key.name)
        .map(a => Single(Contents(a)))

      val result =
        allContents
        .dropWhile(marker.map(ln => (single: Single[Contents]) => single.get.name <= ln))
        .filter(prefix.map(pf => (single: Single[Contents]) => single.get.name.startsWith(pf)))
        .groupByDelimiter(delimiter)
        .truncateByMaxLen(len)

      val groups: Seq[Xmlable] = result.value.map {
        case Single(a) => a
        case Group(a, prefix) => CommonPrefixes(a.map(_.get), prefix)
      }

      val xml =
        <ListBucketResult>
          <Name>{bucketName}</Name>
          { prefix.map(a => <Prefix>{a}</Prefix>).getOrElse(<Prefix></Prefix>) }
          { marker.map(a => <Marker>{a}</Marker>).getOrElse(<Marker></Marker>) }
          { maxKeys.map(a => <MaxKeys>{a}</MaxKeys>).getOrElse(<MaxKeys>1000</MaxKeys>) }
          { delimiter.map(a => <Delimiter>{a}</Delimiter>).getOrElse(NodeSeq.Empty) }
          // [spec] When response is truncated (the IsTruncated element value in the response is true),
          // you can use the key name in this field as marker in the subsequent request to get next set of objects.
          { result.nextMarker.filter(_ => delimiter.isDefined).map(a => <NextMarker>{a.get.name}</NextMarker>).getOrElse(NodeSeq.Empty) }
          <IsTruncated>{result.truncated}</IsTruncated>
          { for (g <- groups) yield g.toXML }
        </ListBucketResult>

      val headers = ResponseHeaderList.builder
        .withHeader(X_AMZ_REQUEST_ID, requestId)
        .build

      complete(StatusCodes.OK, headers, xml)
    }
  }
}
