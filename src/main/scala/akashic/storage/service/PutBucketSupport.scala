package akashic.storage.service

import akashic.storage.Server
import akashic.storage.patch._
import akashic.storage.service.Error.Reportable
import io.finch._

trait PutBucketSupport {
  self: Server =>
  object PutBucket {
    val matcher = put(string ? RequestId.reader ? CallerId.reader).as[t]
    val endpoint = matcher { a: t => a.run }

    case class t(bucketName: String, requestId: String, callerId: String) extends Task[Output[Unit]] with Reportable {
      def resource = Resource.forBucket(bucketName)
      def runOnce = {
        val created = Commit.once(tree.bucketPath(bucketName)) { patch =>
          val bucketPatch = patch.asBucket
          bucketPatch.init

          Commit.retry(bucketPatch.acl) { patch =>
            val dataPatch = patch.asData
            dataPatch.init

            dataPatch.writeBytes(Acl.t(callerId, Seq(
              Acl.Grant(
                Acl.ById(callerId),
                Acl.FullControl()
              )
            )).toBytes)
          }
          Commit.retry(bucketPatch.versioning) { patch =>
            val dataPatch = patch.asData
            dataPatch.init

            dataPatch.writeBytes(Versioning.t(Versioning.UNVERSIONED).toBytes)
          }
        }
        if (!created) failWith(Error.BucketAlreadyExists())
        Ok()
          .withHeader(X_AMZ_REQUEST_ID -> requestId)
      }
    }
  }
}
