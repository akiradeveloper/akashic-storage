package akashic.storage.service

import java.util.Date

import akashic.storage.patch.Bucket
import akashic.storage.service.Error.Reportable
import akashic.storage.{server, patch}
import akka.http.scaladsl.model.{StatusCodes, HttpRequest}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._

import scala.xml.NodeSeq

object GetService {
  val matcher = get & extractRequest
  val route = matcher.as(t)(_.run)

  case class t(req: HttpRequest) extends AuthorizedAPI {
    def name = "GET Service"
    def resource = Resource.forRoot

    def runOnce = {
      def Owner(callerId: String) = {
        <Owner>
          <ID>{callerId}</ID>
          <DisplayName>{server.users.find(callerId).get.displayName}</DisplayName>
        </Owner>
      }
      def Bucket(b: patch.Bucket) = {
        val date = new Date(b.root.getAttr.creationTime)
        val creationDate = dates.format000Z(date)
        <Bucket>
          <Name>{b.name}</Name>
          <CreationDate>{creationDate}</CreationDate>
        </Bucket>
      }

      if (callerId == "") {
        failWith(Error.AccessDenied())
      }

      val allBuckets: Iterable[Bucket] = server.tree.listBuckets
      val listing = allBuckets.filter { bucket =>
        bucket.acl.get.owner == callerId
      }

      val xml =
        <ListAllMyBucketsResult>
          {Owner(callerId)}
          <Buckets>
            {listing.map(Bucket)}
          </Buckets>
        </ListAllMyBucketsResult>

      val headers = ResponseHeaderList.builder
        .withHeader(X_AMZ_REQUEST_ID, requestId)
        .build

      complete(StatusCodes.OK, headers, xml)
    }
  }
}
