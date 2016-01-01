package akashic.api

object GetService {
  case class Result(xml: NodeSeq)
}

case class GetService extends Task[GetService.Result] {
  def doRun =
    <ListAllMyBucketsResult>
      <Owner>
        <ID>{callerId.get}</ID>
        <DisplayName>{users.getUser(callerId.get).get.displayName}</DisplayName>
      </Owner>
      <Buckets>
        { for (b <- tree.listBuckets if b.completed) yield
        <Bucket>
          <Name>{b.path.lastName}</Name>
          <CreationDate>{b.path.lastModified.format000Z}</CreationDate>
        </Bucket>
        }
      </Buckets>
    </ListAllMyBucketsResult>
}
