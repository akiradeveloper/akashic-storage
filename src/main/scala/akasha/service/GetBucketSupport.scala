package akasha.service

import akasha.patch.Version
import io.finch._
import akasha.{files, Server}
import akasha.service.Error.Reportable
import scala.xml.NodeSeq

trait GetBucketSupport {
  self: Server =>
  object GetBucket {
    val matcher = get(string ?
      paramOption("delimiter") ?
      paramOption("encoding-type") ?
      paramOption("marker") ?
      paramOption("max-keys").as[Int] ?
      paramOption("prefix") ?
      RequestId.reader ?
      CallerId.reader
    ).as[t]
    val endpoint = matcher { a: t => a.run }
    case class t(bucketName: String,
                 delimiter: Option[String],
                 encodingType: Option[String],
                 marker: Option[String],
                 maxKeys: Option[Int],
                 prefix: Option[String],
                 requestId: String,
                 callerId: String) extends Task[Output[NodeSeq]] with Reportable {
      def resource = Resource.forBucket(bucketName)
      def runOnce = {
        sealed trait Group {
          def toXML: NodeSeq
          def lastKeyName: String // last keyname in this group
        }
        case class Contents(version: Version) extends Group {
          val acl = Acl.fromBytes(version.acl.get.get.asData.readBytes)
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
                <DisplayName>{users.getUser(ownerId).get.displayName}</DisplayName>
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

        val bucket = tree.findBucket(bucketName) match {
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
       
        val groups: Seq[Contents] = bucket.listKeys
          .map(_.findLatestVersion)
          .filter(_.isDefined).map(_.get) // List[Version]
          .filter { version =>
            val meta = Meta.fromBytes(version.meta.readBytes)
            !meta.isDeleteMarker
          }
          .sortBy(_.key.name) 
          .applySome(marker) { a => b => a.dropWhile(_.key.name < b) }
          .applySome(prefix) { a => b => a.filter(_.key.name.startsWith(b)) }
          .map(Contents(_))
          // TODO support delimiter. use CommonPrefixes

        val len = maxKeys match {
          case Some(a) => a
          case None => 1000 // dafault
        } 

        val truncated = groups.size > len

        // not used
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
            <IsTruncated>{truncated}</IsTruncated>
            { for (g <- groups) yield g.toXML }
          </ListBucketResult>
        Ok(xml)
          .withHeader("x-amz-requestId" -> requestId)
      }
    }
  }
}
