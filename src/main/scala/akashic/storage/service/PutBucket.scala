package akashic.storage.service

import java.nio.file.Files

import akashic.storage.server
import akashic.storage.patch._
import akashic.storage.service.Acl.Grant
import akashic.storage.service.Error.Reportable
import akka.http.scaladsl.model.{StatusCodes, HttpEntity, HttpRequest}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._

import scala.xml.{XML, NodeSeq}

object PutBucket {
  val matcher =
    put &
    extractBucket &
    optionalHeaderValueByName("x-amz-acl") &
    extractGrantsFromHeaders &
    optionalStringBody &
    extractRequest
  val route = matcher.as(t)(_.run)
  case class t(bucketName: String,
               cannedAcl: Option[String],
               grantsFromHeaders: Iterable[Grant],
               entity: Option[String],
               req: HttpRequest) extends AuthorizedAPI {
    def name = "PUT Bucket"
    def resource = Resource.forBucket(bucketName)
    def runOnce = {
      val dest = server.tree.bucketPath(bucketName)

      if (Files.exists(dest)) {
        val bucket = findBucket(server.tree, bucketName)
        val bucketAcl = bucket.acl.get
        if (bucketAcl.owner == callerId) {
          failWith(Error.BucketAlreadyOwnByYou())
        } else {
          failWith(Error.BucketAlreadyExists())
        }
      }

      Commit.once(dest) { patch =>
        val bucketPatch = patch.asBucket
        bucketPatch.init

        Commit.replaceData(bucketPatch.acl, Acl.makeCache) { data =>
          val grantsFromCanned = (cannedAcl <+ Some("private")).map(Acl.CannedAcl.forName(_, callerId, callerId)).map(_.makeGrants).get
          data.put(Acl.t(callerId, grantsFromCanned ++ grantsFromHeaders))
        }

        Commit.replaceData(bucketPatch.versioning, Versioning.makeCache) { data =>
          data.put(Versioning.t(Versioning.UNVERSIONED))
        }

        // [spec] empty string (for the US East (N. Virginia) region)
        val loc: Option[String] = entity.map(XML.loadString).map(parseLocationConstraint) <+ Some("")
        Commit.replaceData(bucketPatch.location, Location.makeCache) { data =>
          data.put(Location.t(loc.get))
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
