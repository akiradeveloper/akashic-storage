package akasha.service

import akasha.Server
import akasha.patch._
import akasha.service.Error.Reportable
import io.finch._

trait PutBucketSupport {
  self: Server =>
  object PutBucket {
    val matcher = put(string ? RequestId.reader ? CallerId.reader).as[t]
    val endpoint = matcher { a: t => a.run }

    case class t(bucketName: String, requestId: String, callerId: String) extends Task[Output[Unit]] with Reportable {
      def resource = bucketName
      def runOnce = {
        val created = Commit.Once(tree.bucketPath(bucketName)) { patch =>
          val bucketPatch = patch.asBucket
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
        Ok()
          .withHeader(("x-amz-request-id", requestId))
      }
    }
  }
}
