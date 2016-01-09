package akasha.service

import akasha.patch._

object PutBucket {
  case class Input(bucketName: String)
  case class Output()
}

trait PutBucket { self: Context =>
  import akasha.service.PutBucket._
  case class PutBucket(input: Input) extends Task[Output] {
    def doRun = {
      val created = Commit.Once(tree.bucketPath(input.bucketName)) { patch =>
        val bucketPatch: Bucket = Bucket(patch.root)
        bucketPatch.init
        Commit.Retry(bucketPatch.acl) { patch =>
          val dataPatch = patch.asData
          dataPatch.writeBytes(Acl.t(callerId, Seq(
            Acl.Grant(
              Acl.ById(callerId),
              Acl.FullControl()
            )
          )).toBytes)
        }.run
        Commit.Retry(bucketPatch.versioning) { patch =>
          val dataPatch = patch.asData
          dataPatch.writeBytes(Versioning.t(Versioning.UNVERSIONED).toBytes)
        }.run
      }.run
      if (!created) failWith(Error.BucketAlreadyExists())
      Output()
    }
  }
  def doPutBucket(input: Input) = PutBucket(input).run
}
