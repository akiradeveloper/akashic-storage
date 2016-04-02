package akashic.storage.service

import java.util.Date

import akashic.storage.auth.CallerId
import akashic.storage.patch.Bucket
import akashic.storage.{patch, server}
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._

object GetService {
  val matcher = get & provide(())

  val route = matcher.as(t)(_.run)

  case class t(n: Unit) extends AuthorizedAPI {
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

      if (callerId == CallerId.ANONYMOUS) {
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
