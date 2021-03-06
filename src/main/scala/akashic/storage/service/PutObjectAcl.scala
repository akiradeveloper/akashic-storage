package akashic.storage.service

import akashic.storage._
import akashic.storage.caching.Cache
import akashic.storage.patch.Commit
import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import scala.xml.XML

object PutObjectAcl {
  val matcher =
    put &
    extractObject &
    withParameter("acl") &
    optionalHeaderValueByName("x-amz-acl") &
    extractGrantsFromHeaders &
    optionalStringBody

  val route = matcher.as(t)(_.run)

  case class t(bucketName: String, keyName: String,
               cannedAcl: Option[String],
               grantsFromHeaders :Iterable[Acl.Grant],
               xmlString: Option[String]) extends AuthorizedAPI {
    override def name: String = "PUT Object ACL"
    override def resource: String = Resource.forObject(bucketName, keyName)
    override def runOnce: Route = {
      val bucket = findBucket(server.tree, bucketName)
      val key = findKey(bucket, keyName)
      val version = key.findLatestVersion match {
        case Some(a) => a
        case None => failWith(Error.NoSuchKey())
      }
      val versionAcl = version.acl.get
      if (!versionAcl.grant(callerId, Acl.WriteAcp()))
        failWith(Error.AccessDenied())

      val newAcl = if (xmlString.isDefined) {
        val xml = XML.loadString(xmlString.get)
        Acl.parseXML(xml)
      } else if (!grantsFromHeaders.isEmpty) {
        Acl(versionAcl.owner, grantsFromHeaders)
      } else {
        val owner = versionAcl.owner
        val grantsFromCanned = (cannedAcl <+ Some("private")).map(Acl.CannedAcl.forName(_, owner, owner)).map(_.makeGrants).get
        Acl(versionAcl.owner, grantsFromCanned)
      }

      Commit.replaceData(version.acl, Acl.makeCache) { data =>
        data.replace(newAcl, Cache.creationTimeOf(version.acl.filePath))
      }

      val headers = ResponseHeaderList.builder
        .withHeader(X_AMZ_REQUEST_ID, requestId)
        .build

      complete(StatusCodes.OK, headers, HttpEntity.Empty)
    }
  }
}
