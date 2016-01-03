package akasha.api

object GetService {
  case class Result(xml: NodeSeq)
}

case class GetService(callerId: Option[String]) extends Task[GetService.Result] {
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
