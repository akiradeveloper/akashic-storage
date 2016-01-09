package akasha.service

import akasha.{FileOps, patch}

import scala.xml.NodeSeq

object GetService {
  case class Input()
  case class Output(xml: NodeSeq)
}

trait GetService { self: Context =>
  import akasha.service.GetService._
  case class GetService(input: Input) extends Task[Output] {
    def doRun = {
      def Owner(callerId: String) = {
        <Owner>
          <ID>{callerId}</ID>
          <DisplayName>{users.getUser(callerId).get.displayName}</DisplayName>
        </Owner>
      }
      def Bucket(b: patch.Bucket) = {
        val date = FileOps.lastDate(b.root)
        val creationDate = DateOps.format000Z(date)
        <Bucket>
          <Name>{b.name}</Name>
          <CreationDate>{creationDate}</CreationDate>
        </Bucket>
      }
      val xml =
        <ListAllMyBucketsResult>
          { Owner(callerId) }
          <Buckets>
            { for (b <- tree.listBuckets) yield Bucket(b) }
          </Buckets>
        </ListAllMyBucketsResult>

      Output(xml)
    }
  }
  def doGetService(input: Input) = GetService(input).run
}
