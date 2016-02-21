package akashic.storage.service

import akashic.storage.patch.{Data, Commit}
import akka.http.scaladsl.model.{HttpEntity, StatusCodes, HttpRequest}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import akashic.storage.server

import scala.xml.XML

object PutBucketAcl {
  val matcher =
    get &
    extractBucket &
    withParamter("acl") &
    optionalHeaderValueByName("x-amz-acl") &
    extractGrantsFromHeaders &
    entity(as[Option[String]]) &
    extractRequest

  val route = matcher.as(t)(_.run)

  case class t(bucketName: String,
               cannedAcl: Option[String],
               grantsFromHeaders: Iterable[Acl.Grant],
               body: Option[String],
               req: HttpRequest) extends AuthorizedAPI {
    override def name: String = "PUT Bucket ACL"
    override def resource: String = Resource.forBucket(bucketName)
    override def runOnce: Route = {
      val bucket = findBucket(server.tree, bucketName)
      val bucketAcl = Acl.fromBytes(bucket.acl.read)
      if (!bucketAcl.getPermission(callerId).contains(Acl.WriteAcp()))
        failWith(Error.AccessDenied())

      val newAcl = if (body.isDefined) {
        val xml = XML.loadString(body.get)
        Acl.parseXML(xml)
      } else if (!grantsFromHeaders.isEmpty) {
        Acl.t(bucketAcl.owner, grantsFromHeaders)
      } else {
        val owner = bucketAcl.owner
        val grantsFromCanned = (cannedAcl <+ Some("private")).map(Acl.CannedAcl.forName(_, owner, owner)).map(_.makeGrants).get
        Acl.t(bucketAcl.owner, grantsFromCanned)
      }

      Commit.replaceData(bucket.acl) { data: Data =>
        data.write(newAcl.toBytes)
      }
      val headers = ResponseHeaderList.builder
        .withHeader(X_AMZ_REQUEST_ID, requestId)
        .build
      complete(StatusCodes.OK, headers, HttpEntity.Empty)
    }
  }
}
