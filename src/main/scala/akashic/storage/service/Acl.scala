package akashic.storage.service

import scala.pickling.Defaults._
import scala.pickling.binary._
import scala.xml.NodeSeq
import akashic.storage.server

object Acl {
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
        case ById(id: String) => id == callerId
        case ByEmail(email: String) =>
          server.users.getUser(callerId).get.email == email
        case AuthenticatedUsers() =>
          callerId != "anonymous"
        case AllUsers() =>
          true
      }
    }
    def doToXML: NodeSeq
    def toXML: NodeSeq = {
      <Grantee>
        {doToXML}
      </Grantee>
    }
  }
  case class ById(id: String) extends Grantee {
    override def doToXML: NodeSeq = {
      <ID>{id}</ID>
      <DisplayName>{server.users.getUser(id).get.displayName}</DisplayName>
    }
  }
  case class ByEmail(email: String) extends Grantee {
    override def doToXML: NodeSeq =
      <EmailAddress>{email}</EmailAddress>
  }
  case class AuthenticatedUsers() extends Grantee {
    override def doToXML: NodeSeq =
      <URI>http://acs.amazonaws.com/groups/global/AuthenticatedUsers</URI>
  }
  case class AllUsers() extends Grantee {
    override def doToXML: NodeSeq =
      <URI>http://acs.amazonaws.com/groups/global/AllUsers</URI>
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
      def default(owner: String) = Grant(ById(owner), FullControl())
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
}
