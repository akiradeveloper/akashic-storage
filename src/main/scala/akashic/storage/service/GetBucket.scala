package akashic.storage.service

import akashic.storage.patch.Version
import com.twitter.finagle.http.Request
import io.finch._
import akashic.storage.{files, server}
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
        def prefixBy(delimiter: String): String = {
          val s = key.name
          val t = s.split(delimiter)(0)
          if (t == s) s else t + delimiter
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

      implicit class _ApplySome[A](a: A) {
        def applySome[B](bOpt: Option[B])(fn: A => B => A): A = {
          bOpt match {
            case Some(b) => fn(a)(b)
            case None => a
          }
        }
      }
     
      val groups0: Seq[Contents] = bucket.listKeys
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

      val len = maxKeys match {
        case Some(a) => a
        case None => 1000 // dafault
      } 

      // [spec] All of the keys rolled up in a common prefix count as a single return when calculating the number of returns.
      // So truncate the list after grouping into CommonPrefixes
      val groups = groups1.take(len)

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
          { delimiter match { case Some(a) if truncated => <NextMarker>{nextMarker.get}</NextMarker>; case _ => NodeSeq.Empty } }
          <IsTruncated>{truncated}</IsTruncated>
          { for (g <- groups) yield g.toXML }
        </ListBucketResult>
      Ok(xml)
        .withHeader(X_AMZ_REQUEST_ID -> requestId)
    }
  }
}
