package akashic.storage.service

import akashic.storage.patch.Bucket
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._

import akashic.storage.server
import scala.xml.NodeSeq

object ListMultipartUploads {
  case class t(bucketName: String, req: HttpRequest) extends AuthorizedAPI {
    override def name: String = "List Multipart Uploads"

    override def resource: String = Resource.forBucket(bucketName)

    sealed trait Xmlable {
      def toXML: NodeSeq
    }
    case class Upload(keyName: String, uploadId: String) extends Xmlable with BucketListing.Filterable {
      override def name: String = ???
      override def toXML: NodeSeq = ???
    }
    case class CommonPrefixes(uploads: Seq[Upload], prefix: String) extends Xmlable {
      override def toXML: NodeSeq = ???
    }

    override def runOnce: Route = {
      val bucket: Bucket = server.tree.findBucket(bucketName) match {
        case Some(a) => a
        case None => failWith(Error.NoSuchBucket())
      }
      val bucketAcl = bucket.acl.get
      if (!bucketAcl.grant(callerId, Acl.Read()))
        failWith(Error.AccessDenied())

      val allUploads: Seq[Upload] = bucket.listKeys.toSeq
        .sortBy(_.name)
        .map(key => key.uploads.listUploads.map(_.name).toSeq.sorted.map(Upload(key.name, _)))
        .flatten

      complete("")
    }
  }
}
