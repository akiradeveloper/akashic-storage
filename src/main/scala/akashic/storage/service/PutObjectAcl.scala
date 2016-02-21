package akashic.storage.service

import akashic.storage._
import akashic.storage.patch.Commit
import akka.http.scaladsl.model.{HttpEntity, StatusCodes, HttpRequest}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import scala.xml.XML

object PutObjectAcl {
  val matcher =
    put &
    extractObject &
    withParamter("acl") &
    optionalHeaderValueByName("x-amz-acl") &
    extractGrantsFromHeaders &
    entity(as[Option[String]]) &
    extractRequest

  val route = matcher.as(t)(_.run)

  case class t(bucketName: String, keyName: String,
               cannedAcl: Option[String],
               grantsFromHeaders :Iterable[Acl.Grant],
               xmlString: Option[String],
               req: HttpRequest) extends AuthorizedAPI {
    override def name: String = "PUT Object ACL"
    override def resource: String = Resource.forObject(bucketName, keyName)
    override def runOnce: Route = {
      val bucket = findBucket(server.tree, bucketName)
      val key = findKey(bucket, keyName)
      val version = key.findLatestVersion match {
        case Some(a) => a
        case None => failWith(Error.NoSuchKey())
      }
      val versionAcl = Acl.fromBytes(version.acl.read)
      if (!versionAcl.getPermission(callerId).contains(Acl.WriteAcp()))
        failWith(Error.AccessDenied())

      val newAcl = if (xmlString.isDefined) {
        val xml = XML.loadString(xmlString.get)
        Acl.parseXML(xml)
      } else if (!grantsFromHeaders.isEmpty) {
        Acl.t(versionAcl.owner, grantsFromHeaders)
      } else {
        val owner = versionAcl.owner
        val grantsFromCanned = (cannedAcl <+ Some("private")).map(Acl.CannedAcl.forName(_, owner, owner)).map(_.makeGrants).get
        Acl.t(versionAcl.owner, grantsFromCanned)
      }

      Commit.replaceData(version.acl) { data =>
        data.write(newAcl.toBytes)
      }

      val headers = ResponseHeaderList.builder
        .withHeader(X_AMZ_REQUEST_ID, requestId)
        .build

      complete(StatusCodes.OK, headers, HttpEntity.Empty)
    }
  }
}