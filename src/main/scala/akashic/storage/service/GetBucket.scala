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
    parameters("delimiter"?, "encoding-type"?, "marker"?, "max-keys"?, "prefix"?) &
    extractRequest
  val route = matcher.as(t)(_.run)
  case class t(bucketName: String,
               delimiter: Option[String],
               encodingType: Option[String],
               marker: Option[String],
               maxKeys: Option[String],
               prefix: Option[String],
               req: HttpRequest) extends AuthorizedAPI {
    def name = "GET Bucket"
    def resource = Resource.forBucket(bucketName)
    def runOnce = {
      sealed trait Group {
        def toXML: NodeSeq
        def lastKeyName: String // last keyname in this group
      }
      case class Contents(version: Version) extends Group {
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
        override def lastKeyName = key.name
        def prefixBy(delimiter: String): String = {
          val s = key.name
          s.indexOf(delimiter) match {
            case -1 => s
            case i => s.slice(0, i) + delimiter
          }
        }
      }
      // not used yet
      // FIXME use non empty list
      case class CommonPrefixes(members: Seq[Contents], prefix: String) extends Group {
        override def toXML = {
          <CommonPrefixes>
            <Prefix>{prefix}</Prefix>
          </CommonPrefixes>
        }
        override def lastKeyName = {
          members.last.key.name
        }
      }

      val bucket = server.tree.findBucket(bucketName) match {
        case Some(a) => a
        case None => failWith(Error.NoSuchBucket())
      }
      val bucketAcl = Acl.fromBytes(bucket.acl.read)
      if (!bucketAcl.getPermission(callerId).contains(Acl.Read()))
        failWith(Error.AccessDenied())

      implicit class _ApplySome[A](a: A) {
        def applySome[B](bOpt: Option[B])(fn: A => B => A): A = {
          bOpt match {
            case Some(b) => fn(a)(b)
            case None => a
          }
        }
      }
     
      val groups0: Seq[Contents] = bucket.listKeys
        .map(_.findLatestVersion)
        .filter(_.isDefined).map(_.get) // List[Version]
        .filter { version =>
          val meta = Meta.fromBytes(version.meta.read)
          !meta.isDeleteMarker
        }
        .sortBy(_.key.name) 
        // [spec] Indicates where in the bucket listing begins. Marker is included in the response if it was sent with the request.
        .applySome(marker) { a => b => a.dropWhile(_.key.name <= b) }
        .applySome(prefix) { a => b => a.filter(_.key.name.startsWith(b)) }
        .map(Contents(_))

      val groups1: Seq[Group] = if (delimiter.isEmpty) {
        groups0
      } else {
        val deli = encodeKeyName(delimiter.get)
        groups0.groupBy(_.prefixBy(deli)).toSeq // [prefix -> seq(contents)]
          .sortBy(_._1) // sort by prefix
          .map { case (prefix, members) =>
            if (members.size > 1) {
              CommonPrefixes(members, prefix.slice(0, prefix.size - deli.size))
            } else {
              members(0)
            }
          }
      }

      def parseMaxKeys(s: String): Int = {
        if (s == "") {
          failWith(Error.InvalidArgument())
        }
        Try(s.toInt).toOption match {
          case Some(a) => a
          case None => failWith(Error.InvalidArgument())
        }
      }

      val len = maxKeys match {
        case Some(a) => parseMaxKeys(a)
        case None => 1000 // dafault
      } 

      val truncated = groups1.size > len

      // [spec] All of the keys rolled up in a common prefix count as a single return when calculating the number of returns.
      // So truncate the list after grouping into CommonPrefixes
      val groups: Seq[Group] = groups1.take(len)

      // [spec] This element is returned only if you have delimiter request parameter specified.
      // If response does not include the NextMaker and it is truncated,
      // you can use the value of the last Key in the response as the marker in the subsequent request
      // to get the next set of object keys.
      val nextMarker = truncated match {
        case true if len > 0 => Some(groups(len-1).lastKeyName)
        case _ => None
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
          { delimiter match { case Some(a) if truncated => <NextMarker>{nextMarker.get}</NextMarker>; case _ => NodeSeq.Empty } }
          <IsTruncated>{truncated}</IsTruncated>
          { for (g <- groups) yield g.toXML }
        </ListBucketResult>

      val headers = ResponseHeaderList.builder
        .withHeader(X_AMZ_REQUEST_ID, requestId)
        .build

      complete(StatusCodes.OK, headers, xml)
    }
  }
}
