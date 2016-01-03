package akasha.model

object GetService {
  case class Result(xml: NodeSeq)
}

trait GetService { self: Context =>
  case class GetService() extends Task[GetService.Result] {
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
              <Name>{b.root.lastName}</Name>
              <CreationDate>{b.root.lastModified.format000Z}</CreationDate>
            </Bucket>
            }
          </Buckets>
        </ListAllMyBucketsResult>

      GetService.Result(xml)
    }
  }
  def doGetService = GetService().run
}
