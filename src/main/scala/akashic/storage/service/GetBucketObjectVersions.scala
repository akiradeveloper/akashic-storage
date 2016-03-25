package akashic.storage.service

import java.util.Date

import akashic.storage.patch.Version
import akashic.storage.service.BucketListing.Filterable
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._

import scala.xml.NodeSeq

object GetBucketObjectVersions {

  val matcher =
    get &
    extractBucket &
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
      case class VersionContents(unwrap: Version) extends Xmlable with Filterable {
        override def name: String = unwrap.key.name
        val meta = unwrap.meta.get
        val acl = unwrap.acl.get
        def asNormalVersion: NodeSeq =
          <Version>
            <Key>{name}</Key>
            <VersionId>{unwrap.name}</VersionId>
            <IsLatest>true</IsLatest>
            <LastModified>{dates.format000Z(new Date(unwrap.root.getAttr.creationTime))}</LastModified>
            <ETag>{quoteString(meta.eTag)}</ETag>
            <Size>{unwrap.data.length}</Size>
            <Owner>
              <ID>{acl.owner}</ID>
            </Owner>
            <StorageClass>STANDARD</StorageClass>
          </Version>
        def asDeleteMarker: NodeSeq =
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
      case class CommonPrefixes(members: Seq[VersionContents], prefix: String) extends Xmlable {
        override def toXML = {
          <CommonPrefixes>
            <Prefix>{prefix}</Prefix>
          </CommonPrefixes>
        }
      }
      complete("")

    }
  }
}
