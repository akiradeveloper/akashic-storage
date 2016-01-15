package akashic.storage.service

import akashic.storage.service.Error.Reportable
import akashic.storage.{Server, files, patch}
import io.finch._

import scala.xml.NodeSeq

trait GetServiceSupport {
  self: Server =>

  object GetService {
    val matcher = get(/ ? RequestId.reader ? CallerId.reader).as[t]
    def endpoint: Endpoint[NodeSeq] = matcher { a: t => a.run }

    case class t(requestId: String, callerId: String) extends Task[Output[NodeSeq]] with Reportable {
      def resource = Resource.forRoot

      def runOnce = {
        def Owner(callerId: String) = {
          <Owner>
            <ID>{callerId}</ID>
            <DisplayName>{users.getUser(callerId).get.displayName}</DisplayName>
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
              {for (b <- tree.listBuckets) yield Bucket(b)}
            </Buckets>
          </ListAllMyBucketsResult>

        Ok(xml)
          .withHeader(X_AMZ_REQUEST_ID -> requestId)
      }
    }
  }
}

