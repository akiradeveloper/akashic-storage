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
    withParamter("acl") &
    extractBucket &
    entity(as[String]) &
    extractRequest

  val route = matcher.as(t)(_.run)

  case class t(bucketName: String,
               body: String,
               req: HttpRequest) extends AuthorizedAPI {
    override def name: String = "PUT Bucket ACL"
    override def resource: String = Resource.forBucket(bucketName)
    override def runOnce: Route = {
      val bucket = findBucket(server.tree, bucketName)
      val bucketAcl = Acl.fromBytes(bucket.acl.read)
      if (!bucketAcl.getPermission(callerId).contains(Acl.WriteAcp()))
        failWith(Error.AccessDenied())
      val xml = XML.loadString(body)
      val newAcl = Acl.parseXML(xml)
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
