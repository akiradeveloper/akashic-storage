package akashic.storage.service

import java.nio.file.Path

import akashic.storage.caching.{CacheMap, Cache}

import scala.pickling.Defaults._
import scala.pickling.binary._
import scala.xml.NodeSeq
import akashic.storage.server

object Acl {
  def writer(a: t): Array[Byte] = a.toBytes
  def reader(a: Array[Byte]): t = fromBytes(a)
  def makeCache(path: Path) = new Cache[Acl.t] {
    override val filePath: Path = path
    override def writer: (Acl.t) => Array[Byte] = Acl.writer
    override def reader: (Array[Byte]) => Acl.t = Acl.reader
    override def cacheMap: CacheMap[K, Acl.t] = new CacheMap.Null[K, Acl.t]()
  }
  case class t(owner: String, grants: Iterable[Grant]) {
    def toBytes: Array[Byte] = this.pickle.value
    def getPermission(callerId: String): Set[Permission] = {
      grants.foldLeft(Set.empty[Permission])((acc, grant) => acc ++ grant.getPermission(callerId))
    }
    def ownerXML = {
      val displayName = server.users.getUser(owner).get.displayName
      <Owner>
        <ID>{owner}</ID>
        <DisplayName>{displayName}</DisplayName>
      </Owner>
    }
    def toXML = {
      <AccessControlPolicy>
        {ownerXML}
        <AccessControlList>
          { for (grant <- grants) yield grant.toXML }
        </AccessControlList>
      </AccessControlPolicy>
    }
  }
  def fromBytes(bytes: Array[Byte]): t = BinaryPickle(bytes).unpickle[t]

  case class Grant(grantee: Grantee, perm: Permission) {
    def getPermission(callerId: String): Set[Permission] = {
      grantee.permit(callerId) match {
        case true => perm.dissolve
        case false => Set()
      }
    }
    def toXML: NodeSeq = {
      <Grant>
        {grantee.toXML}
        {perm.toXML}
      </Grant>
    }
  }

  sealed trait Grantee {
    def permit(callerId: String): Boolean = {
      this match {
        case ById(id: String) => {
          id match {
            case "anonymous" =>
              true
            case _ =>
              id == callerId
          }
        }
        case ByEmail(email: String) =>
          server.users.getUser(callerId).get.email == email
        case AuthenticatedUsers() =>
          callerId != "anonymous"
        case AllUsers() =>
          true
      }
    }
    def toXML: NodeSeq
  }
  val URI_AUTHENTICATED_USERS = "http://acs.amazonaws.com/groups/global/AuthenticatedUsers"
  val URI_ALL_USERS = "http://acs.amazonaws.com/groups/global/AllUsers"
  case class ById(id: String) extends Grantee {
    override def toXML: NodeSeq = {
      <Garantee xsi:type="CanonicalUser">
        <ID>{id}</ID>
        <DisplayName>{server.users.getUser(id).get.displayName}</DisplayName>
      </Garantee>
    }
  }
  case class ByEmail(email: String) extends Grantee {
    override def toXML: NodeSeq =
      <Garantee xsi:type="GanonicalUser">
        <EmailAddress>{email}</EmailAddress>
      </Garantee>
  }
  case class AuthenticatedUsers() extends Grantee {
    override def toXML: NodeSeq =
      <Garantee xsi:type="Group">
        <URI>{URI_AUTHENTICATED_USERS}</URI>
      </Garantee>
  }
  case class AllUsers() extends Grantee {
    override def toXML: NodeSeq =
      <Garantee xsi:type="Group">
        <URI>{URI_ALL_USERS}</URI>
      </Garantee>
  }

  // not sealed because WriteAcp and Read are allowed to bucket ACL only
  sealed trait Permission {
    def dissolve: Set[Permission] = {
      this match {
        case FullControl() => Set(Write(), Read(), WriteAcp(), ReadAcp())
        case a => Set(a)
      }
    }
    def toXML: NodeSeq = {
      val s = this match {
        case Write() => "WRITE"
        case Read() => "Read"
        case WriteAcp() => "WRITE_ACP"
        case ReadAcp() => "READ_ACP"
        case FullControl() => "FULL_CONTROL"
      }
      <Permission>{s}</Permission>
    }
  }
  case class FullControl() extends Permission
  case class Write() extends Permission
  case class Read() extends Permission // bucket only
  case class WriteAcp() extends Permission // bucket only
  case class ReadAcp() extends Permission

  def parseXML(xml: NodeSeq): t = {
    val owner = (xml \ "Owner" \ "ID").text

    val grants = (xml \ "AccessControlList" \ "Grant").map { a =>
      // TODO
      val grantee = ById((a \ "Grantee" \ "ID").text) // tmp. we assume authenticated canonical user
      val perm = (a \ "Permission").text match {
        case "FULL_CONTROL" => FullControl()
        case "WRITE" => Write()
        case "READ" => Read()
        case "WRITE_ACP" => WriteAcp()
        case "READ_ACP" => ReadAcp()
      }
      Grant(grantee, perm)
    }
    t(owner, grants)
  }

  sealed trait CannedAcl {
    def makeGrants: Set[Grant] = {
      def default(owner: String) = {
        val grantee = owner match {
          case "anonymous" => AllUsers()
          case _ => ById(owner)
        }
        Grant(grantee, FullControl())
      }
      this match {
        case Private(owner: String) => Set(default(owner))
        case PublicRead(owner: String)  => Set(default(owner), Grant(AllUsers(), Read()))
        case PublicReadWrite(owner: String) => Set(default(owner), Grant(AllUsers(), Read()), Grant(AllUsers(), Write()))
        case AuthenticatedRead(owner: String) => Set(default(owner), Grant(AuthenticatedUsers(), Read()))
        case BucketOwnerRead(owner: String, bucketOwner: String) => Set(default(owner), Grant(ById(bucketOwner), Read()))
        case BucketOwnerFullControl(owner: String, bucketOwner: String) => Set(default(owner), Grant(ById(bucketOwner), FullControl()))
      }
    }
  }
  case class Private(owner: String) extends CannedAcl
  case class PublicRead(owner: String) extends CannedAcl
  case class PublicReadWrite(owner: String) extends CannedAcl
  case class AuthenticatedRead(owner: String) extends CannedAcl
  case class BucketOwnerRead(owner: String, bucketOwner: String) extends CannedAcl // object only
  case class BucketOwnerFullControl(owner: String, bucketOwner: String) extends CannedAcl // object only

  object CannedAcl {
    def forName(name: String, owner: String, bucketOwner: String): CannedAcl = {
      name match {
        case "private" => Private(owner)
        case "public-read" => PublicRead(owner)
        case "public-read-write" => PublicReadWrite(owner)
        case "authenticated-read" => AuthenticatedRead(owner)
        case "bucket-owner-read" => BucketOwnerRead(owner, bucketOwner)
        case "bucket-owner-full-control" => BucketOwnerFullControl(owner, bucketOwner)
      }
    }
  }

  sealed trait GrantHeader {
    def grantees: Iterable[Grantee]
    def permission: Permission
    def makeGrants: Iterable[Grant] = grantees.map(Grant(_, permission))
  }
  case class GrantRead(grantees: Iterable[Grantee]) extends GrantHeader {
    val permission = Read()
  }
  case class GrantWrite(grantees: Iterable[Grantee]) extends GrantHeader {
    val permission = Write()
  }
  case class GrantReadAcp(grantees: Iterable[Grantee]) extends GrantHeader {
    val permission = ReadAcp()
  }
  case class GrantWriteAcp(grantees: Iterable[Grantee]) extends GrantHeader {
    val permission = WriteAcp()
  }
  case class GrantFullControl(grantees: Iterable[Grantee]) extends GrantHeader {
    val permission = FullControl()
  }
  object GrantHeader {
    def doParseLine(k: String, v: String): Grantee = {
      k match {
        case "id" => ById(v)
        case "emailAddress" => ByEmail(v)
        case "uri" => v match {
          case URI_AUTHENTICATED_USERS => AuthenticatedUsers()
          case URI_ALL_USERS => AllUsers()
        }
      }
    }
    def parseLine(k: String, v: String): GrantHeader = {
      val grantees = v
        .split(",")
        .map(_.split("="))
        .map(a => doParseLine(a(0), a(1)))
      k match {
        case "x-amz-grant-read" => GrantRead(grantees)
        case "x-amz-grant-write" => GrantWrite(grantees)
        case "x-amz-grant-read-acp" => GrantReadAcp(grantees)
        case "x-amz-grant-write-acp" => GrantWriteAcp(grantees)
        case "x-amz-grant-full-control" => GrantFullControl(grantees)
      }
    }
  }
}
