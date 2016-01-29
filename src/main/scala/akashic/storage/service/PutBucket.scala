package akashic.storage.service

import java.nio.file.Files

import akashic.storage.server
import akashic.storage.patch._
import akashic.storage.service.Error.Reportable
import com.twitter.finagle.http.Request
import io.finch._

object PutBucket {
  val matcher = put(string ? extractRequest).as[t]
  val endpoint = matcher { a: t => a.run }

  case class t(bucketName: String, req: Request) extends Task[Output[Unit]] {
    def name = "PUT Bucket"
    def resource = Resource.forBucket(bucketName)
    def runOnce = {
      val dest = server.tree.bucketPath(bucketName)

      if (Files.exists(dest))
        failWith(Error.BucketAlreadyExists())

      Commit.once(dest) { patch =>
        val bucketPatch = patch.asBucket
        bucketPatch.init

        Commit.replace(bucketPatch.acl) { patch =>
          val dataPatch = patch.asData
          dataPatch.writeBytes(Acl.t(callerId, Seq(
            Acl.Grant(
              Acl.ById(callerId),
              Acl.FullControl()
            )
          )).toBytes)
        }

        Commit.replace(bucketPatch.versioning) { patch =>
          val dataPatch = patch.asData
          dataPatch.writeBytes(Versioning.t(Versioning.UNVERSIONED).toBytes)
        }
      }
      Ok()
        .withHeader(X_AMZ_REQUEST_ID -> requestId)
    }
  }
}
