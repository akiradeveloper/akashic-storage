package akashic.storage.service

import akashic.storage.patch.Version
import akka.http.scaladsl.model.{StatusCodes, HttpRequest}
import akka.http.scaladsl.server.Directives._
import akashic.storage.{files, server}
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
        val acl = Acl.fromBytes(version.acl.read)
        val meta = Meta.fromBytes(version.meta.read)
        val ownerId = acl.owner
        val key = version.key
        override def toXML = {
          <Contents>
            <Key>{decodeKeyName(key.name)}</Key>
            <LastModified>{dates.format000Z(files.lastDate(version.root))}</LastModified>
            <ETag>{quoteString(meta.eTag)}</ETag>
            <Size>{version.data.length}</Size>
            <StorageClass>STANDARD</StorageClass>
            <Owner>
              <ID>{ownerId}</ID>
              <DisplayName>{server.users.getUser(ownerId).get.displayName}</DisplayName>
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
      val bucketAcl = Acl.fromBytes(bucket.acl.read)
      if (!bucketAcl.getPermission(callerId).contains(Acl.Read()))
        failWith(Error.AccessDenied())

      import BucketListing._
      val len = maxKeys match {
        case Some(a) => a
        case None => 1000 // dafault
      }

      val result = bucket.listKeys
        .map(_.findLatestVersion)
        .filter(_.isDefined).map(_.get) // List[Version]
        .filter { version =>
          val meta = Meta.fromBytes(version.meta.read)
          !meta.isDeleteMarker
        }
        .sortBy(_.key.name)
        .map(a => Single(Contents(a)))
        .takesOnlyAfter(marker)
        .filterByPrefix(prefix)
        .groupByDelimiter(delimiter)
        .truncateByMaxLen(len)

      val groups: Seq[Xmlable] = result.value.map {
        case Single(a) => a
        case Group(a, prefix) => CommonPrefixes(a.map(_.get), prefix)
      }

      val xml =
        <ListBucketResult>
          <Name>{bucketName}</Name>
          { prefix match { case Some(a) => <Prefix>{a}</Prefix>; case None => <Prefix></Prefix> } }
          { marker match { case Some(a) => <Marker>{a}</Marker>; case None => <Marker></Marker> } }
          { maxKeys match { case Some(a) => <MaxKeys>{a}</MaxKeys>; case None => <MaxKeys>1000</MaxKeys> } }
          { delimiter match { case Some(a) => <Delimiter>{a}</Delimiter>; case None => NodeSeq.Empty } }
          // [spec] When response is truncated (the IsTruncated element value in the response is true),
          // you can use the key name in this field as marker in the subsequent request to get next set of objects.
          { delimiter match { case Some(a) if result.truncated => <NextMarker>{result.nextMarker.get}</NextMarker>; case _ => NodeSeq.Empty } }
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
