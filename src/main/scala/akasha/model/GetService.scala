package akasha.model

import akasha.patch
import scala.xml.NodeSeq

object GetService {
  case class Input()
  case class Output(xml: NodeSeq)
}

trait GetService { self: Context =>
  import akasha.model.GetService._
  case class GetService(input: Input) extends Task[Output] {
    def doRun = {
      def Owner(callerId: Option[String]) = {
        val id = callerId match {
          case Some(a) => a
          case None => "anonymous"
        }
        <Owner>
          <ID>{id}</ID>
          <DisplayName>{users.getUser(callerId.get).get.displayName}</DisplayName>
        </Owner>
      }
      def Bucket(b: patch.Bucket) = {
        val date = akasha.Files.lastDate(b.root)
        val creationDate = Dates.format000Z(date)
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
  def doGetService = GetService(Input()).run
}
