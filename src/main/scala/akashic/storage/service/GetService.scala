package akashic.storage.service

import akashic.storage.service.Error.Reportable
import akashic.storage.{server, files, patch}
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._

import scala.xml.NodeSeq

object GetService {
  // val route = (get & extractRequest)(req => t(req).run)

  val matcher = (get & extractRequest).map(t)
  val route = matcher { a: t => a.run }

  case class t(req: HttpRequest) extends Task[Route] {
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
            {for (b <- server.tree.listBuckets) yield Bucket(b)}
          </Buckets>
        </ListAllMyBucketsResult>

      complete(xml)
    }
  }
}
