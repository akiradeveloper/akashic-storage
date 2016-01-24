package akashic.storage.service

import akashic.storage.service.Error.Reportable
import akashic.storage.{server, files, patch}
import com.twitter.finagle.http.Request
import io.finch._

import scala.xml.NodeSeq

object GetService {
  val matcher = get(/ ? extractRequest).as[t]
  def endpoint: Endpoint[NodeSeq] = matcher { a: t => a.run }

  case class t(req: Request) extends Task[Output[NodeSeq]] {
    def name = "GET Service"
    def resource = Resource.forRoot

    def runOnce = {
      def Owner(callerId: String) = {
        <Owner>
          <ID>{callerId}</ID>
          <DisplayName>{server.users.getUser(callerId).get.displayName}</DisplayName>
        </Owner>
      }
      def Bucket(b: patch.Bucket) = {
        val date = files.lastDate(b.root)
        val creationDate = dates.format000Z(date)
        <Bucket>
          <Name>{b.name}</Name>
          <CreationDate>{creationDate}</CreationDate>
        </Bucket>
      }
      val xml =
        <ListAllMyBucketsResult>
          {Owner(callerId)}
          <Buckets>
            {for (b <- server.tree.listBuckets.filter(_.committed)) yield Bucket(b)}
          </Buckets>
        </ListAllMyBucketsResult>

      Ok(xml)
        .withHeader(X_AMZ_REQUEST_ID -> requestId)
    }
  }
}
