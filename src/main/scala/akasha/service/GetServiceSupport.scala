package akasha.service

import akasha.service.Error.Reportable
import akasha.{Server, files, patch}
import io.finch._

import scala.xml.NodeSeq

trait GetServiceSupport {
  self: Server =>

  object GetService {
    val params = get(/) ? RequestId.reader ? CallerId.reader
    def endpoint: Endpoint[NodeSeq] = params { (requestId: String, callerId: String) =>
      t(requestId, callerId).run
    }

    case class t(requestId: String, callerId: String) extends Task[Output[NodeSeq]] with Reportable {
      def resource = "/"

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
          .withHeader(("x-amz-request-id", requestId))
      }
    }
  }
}

