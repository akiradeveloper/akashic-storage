package akasha.model

import scala.xml.NodeSeq

object GetService {
  case class Result(xml: NodeSeq)
}

trait GetService { self: Context =>
  import akasha.model.GetService._
  case class GetService() extends Task[Result] {
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

      Result(xml)
    }
  }
  def doGetService = GetService().run
}
