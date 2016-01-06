package akasha.model

import akasha.patch._

object PutBucket {
  case class Input(bucketName: String)
  case class Output()
}

trait PutBucket { self: Context =>
  import akasha.model.PutBucket._
  case class PutBucket(input: Input) extends Task[Output] {
    def doRun = {
      // val bucket = tree.findBucket(input.bucketName) match {
      //   case Some(a) => a
      //   case None => failWith(Error.NoSuchBucket())
      // }
      val created = Commit.Once(tree.bucketPath(input.bucketName)) { patch =>
        val bucketPatch: Bucket = Bucket(patch.root)
        bucketPatch.init
        // TODO acl, cors, versioning
      }.run
      if (!created) failWith(Error.BucketAlreadyExists())
      Output()
    }
  }
  def doPutBucket(input: Input) = PutBucket(input).run
}
