package akashic.storage.service

import akashic.storage.patch.Version
import com.twitter.finagle.http.Request
import io.finch._
import akashic.storage.{files, server}
import akashic.storage.service.Error.Reportable
import scala.xml.NodeSeq

object GetBucket {
  val matcher = get(string ?
    paramOption("delimiter") ?
    paramOption("encoding-type") ?
    paramOption("marker") ?
    paramOption("max-keys").as[Int] ?
    paramOption("prefix") ?
    extractRequest
  ).as[t]
  val endpoint = matcher { a: t => a.run }
  case class t(bucketName: String,
               delimiter: Option[String],
               encodingType: Option[String],
               marker: Option[String],
               maxKeys: Option[Int],
               prefix: Option[String],
               req: Request) extends Task[Output[NodeSeq]] {
    def name = "GET Bucket"
    def resource = Resource.forBucket(bucketName)
    def runOnce = {
      sealed trait Group {
        def toXML: NodeSeq
        def lastKeyName: String // last keyname in this group
      }
      case class Contents(version: Version) extends Group {
        val acl = Acl.fromBytes(version.acl.find.get.asData.readBytes)
        val meta = Meta.fromBytes(version.meta.readBytes)
        val ownerId = acl.owner
        val key = version.key
        override def toXML = {
          <Contents>
            <Key>{key.name}</Key>
            <LastModified>{dates.format000Z(files.lastDate(version.root))}</LastModified>
            <ETag>{meta.eTag}</ETag>
            <Size>{version.data.length}</Size>
            <StorageClass>STANDARD</StorageClass>
            <Owner>
              <ID>{ownerId}</ID>
              <DisplayName>{server.users.getUser(ownerId).get.displayName}</DisplayName>
            </Owner>
          </Contents>
        }
        override def lastKeyName = key.name
      }
      // not used yet
      // FIXME use non empty list
      case class CommonPrefixes(versions: List[Version], prefix: String) extends Group {
        override def toXML = {
          <CommonPrefixes>
            <Prefix>{prefix}</Prefix>
          </CommonPrefixes>
        }
        override def lastKeyName = {
          versions.last.key.name
        }
      }
      // FIXME (delim can be any words)
      def computePrefix(s: String, delim: Char): String = {
        val t = s.takeWhile(_ != delim)
        // pure key name doesn't end with '/'
        // a/a (-> a/)
        // a (-> a)
        val slash = if (t == s) { "" } else { "/" }
        t + slash
      }

      val bucket = server.tree.findBucket(bucketName) match {
        case Some(a) => a
        case None => failWith(Error.NoSuchBucket())
      }

      implicit class _ApplySome[A](a: A) {
        def applySome[B](bOpt: Option[B])(fn: A => B => A): A = {
          bOpt match {
            case Some(b) => fn(a)(b)
            case None => a
          }
        }
      }
     
      var groupsNonTruncated: Seq[Contents] = bucket.listKeys
        .filter(_.committed)
        .map(_.findLatestVersion)
        .filter(_.isDefined).map(_.get) // List[Version]
        .filter { version =>
          val meta = Meta.fromBytes(version.meta.readBytes)
          !meta.isDeleteMarker
        }
        .sortBy(_.key.name) 
        // [spec] Indicates where in the bucket listing begins. Marker is included in the response if it was sent with the request.
        .applySome(marker) { a => b => a.dropWhile(_.key.name < b) }
        .applySome(prefix) { a => b => a.filter(_.key.name.startsWith(b)) }
        .map(Contents(_))
        // TODO support delimiter. use CommonPrefixes

      val len = maxKeys match {
        case Some(a) => a
        case None => 1000 // dafault
      } 

      // [spec] All of the keys rolled up in a common prefix count as a single return when calculating the number of returns.
      // So truncate the list after grouping into CommonPrefixes
      val groups = groupsNonTruncated.take(len)

      val truncated = groups.size > len

      // [spec] This element is returned only if you have delimiter request parameter specified.
      // If response does not include the NextMaker and it is truncated,
      // you can use the value of the last Key in the response as the marker in the subsequent request
      // to get the next set of object keys.
      val nextMarker = truncated match {
        case true => Some(groups.last.lastKeyName)
        case false => None
      }

      val xml = 
        <ListBucketResult>
          <Name>{bucketName}</Name>
          { prefix match { case Some(a) => <Prefix>{a}</Prefix>; case None => NodeSeq.Empty } } 
          { marker match { case Some(a) => <Marker>{a}</Marker>; case None => NodeSeq.Empty } }
          { maxKeys match { case Some(a) => <MaxKeys>{a}</MaxKeys>; case None => NodeSeq.Empty } }
          // [spec] When response is truncated (the IsTruncated element value in the response is true),
          // you can use the key name in this field as marker in the subsequent request to get next set of objects.
          { delimiter match { case Some(a) if truncated => <NextMarker>{nextMarker}</NextMarker> } }
          <IsTruncated>{truncated}</IsTruncated>
          { for (g <- groups) yield g.toXML }
        </ListBucketResult>
      Ok(xml)
        .withHeader(X_AMZ_REQUEST_ID -> requestId)
    }
  }
}
