package akashic.storage.service

import akashic.storage.patch._
import akashic.storage.server
import akashic.storage.service.Acl.Grant
import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._

import scala.xml.{NodeSeq, XML}

object PutBucket {
  val matcher =
    put &
    extractBucket &
    optionalHeaderValueByName("x-amz-acl") &
    extractGrantsFromHeaders &
    optionalStringBody
  val route = matcher.as(t)(_.run)
  case class t(bucketName: String,
               cannedAcl: Option[String],
               grantsFromHeaders: Iterable[Grant],
               entity: Option[String]) extends AuthorizedAPI {
    def name = "PUT Bucket"
    def resource = Resource.forBucket(bucketName)
    def runOnce = {
      val dest = server.tree.bucketPath(bucketName)

      if (dest.exists) {
        val bucket = findBucket(server.tree, bucketName)
        val bucketAcl = bucket.acl.get
        if (bucketAcl.owner == callerId) {
          failWith(Error.BucketAlreadyOwnByYou())
        } else {
          failWith(Error.BucketAlreadyExists())
        }
      }

      Commit.once(dest) { patch =>
        val bucketPatch = Bucket(patch.root)
        bucketPatch.init

        bucketPatch.acl.put {
          val grantsFromCanned = (cannedAcl <+ Some("private"))
            .map(Acl.CannedAcl.forName(_, callerId, callerId))
            .map(_.makeGrants).get
          Acl(callerId, grantsFromCanned ++ grantsFromHeaders)
        }

        bucketPatch.versioning.put {
          Versioning.UNVERSIONED
        }

        bucketPatch.location.put {
          // [spec] empty string (for the US East (N. Virginia) region)
          val loc: Option[String] = entity.map(XML.loadString).map(parseLocationConstraint) <+ Some("")
          Location(loc.get)
        }
      }

      val headers = ResponseHeaderList.builder
        .withHeader(X_AMZ_REQUEST_ID, requestId)
        .build

      complete(StatusCodes.OK, headers, HttpEntity.Empty)
    }
  }
  def parseLocationConstraint(xml: NodeSeq): String = {
    (xml \ "LocationConstraint").text
  }
}
