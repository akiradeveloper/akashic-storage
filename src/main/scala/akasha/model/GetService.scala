package akasha.model

import scala.xml.NodeSeq

object GetService {
  case class Input()
  case class Output(xml: NodeSeq)
}

trait GetService { self: Context =>
  import akasha.model.GetService._
  case class GetService(input: Input) extends Task[Output] {
    def doRun = {
      val xml =
        <ListAllMyBucketsResult>
          <Owner>
            <ID>{callerId.get}</ID>
            <DisplayName>{users.getUser(callerId.get).get.displayName}</DisplayName>
          </Owner>
          <Buckets>
            { for (b <- tree.listBuckets) yield
            <Bucket>
              <Name>{b.name}</Name>
              <CreationDate>{val date = akasha.Files.lastDate(b.root); Dates.format000Z(date)}</CreationDate>
            </Bucket>
            }
          </Buckets>
        </ListAllMyBucketsResult>

      Output(xml)
    }
  }
  def doGetService = GetService(Input()).run
}
