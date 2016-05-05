package akashic.storage.service

import akashic.storage.caching.Cache
import akashic.storage.patch.Commit
import akashic.storage.server
import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import scala.xml.XML

object PutBucketAcl {
  val matcher =
    put &
    extractBucket &
    withParameter("acl") &
    optionalHeaderValueByName("x-amz-acl") &
    extractGrantsFromHeaders &
    optionalStringBody

  val route = matcher.as(t)(_.run)

  case class t(bucketName: String,
               cannedAcl: Option[String],
               grantsFromHeaders: Iterable[Acl.Grant],
               body: Option[String]) extends AuthorizedAPI {
    override def name: String = "PUT Bucket ACL"
    override def resource: String = Resource.forBucket(bucketName)
    override def runOnce: Route = {
      val bucket = findBucket(server.tree, bucketName)
      val bucketAcl = bucket.acl.get
      if (!bucketAcl.grant(callerId, Acl.WriteAcp()))
        failWith(Error.AccessDenied())

      val newAcl = if (body.isDefined) {
        val xml = XML.loadString(body.get)
        Acl.parseXML(xml)
      } else if (!grantsFromHeaders.isEmpty) {
        Acl(bucketAcl.owner, grantsFromHeaders)
      } else {
        val owner = bucketAcl.owner
        val grantsFromCanned = (cannedAcl <+ Some("private")).map(Acl.CannedAcl.forName(_, owner, owner)).map(_.makeGrants).get
        Acl(bucketAcl.owner, grantsFromCanned)
      }

      Commit.replaceData(bucket.acl, Acl.makeCache) { data =>
        data.replace(newAcl, Cache.creationTimeOf(bucket.acl.filePath))
      }
      val headers = ResponseHeaderList.builder
        .withHeader(X_AMZ_REQUEST_ID, requestId)
        .build

      complete(StatusCodes.OK, headers, HttpEntity.Empty)
    }
  }
}
