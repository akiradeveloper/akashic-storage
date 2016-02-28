package akashic.storage.service

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._

object GetBucketObjectVersions {

  val matcher =
    get &
    extractBucket &
    parameters("delimiter"?, "encoding-type"?, "key-marker"?, "max-keys".as[Int]?, "prefix"?, "version-id-marker"?) &
    extractRequest

  val route =
    matcher.as(t)(_.run)

  case class t(bucketName: String,
               delimiter: Option[String],
               encodingType: Option[String],
               keyMarker: Option[String],
               maxKeys: Option[Int],
               prefix: Option[String],
               versionIdMarker: Option[String],
               req: HttpRequest) extends AuthorizedAPI {
    override def name: String = "GET Bucket Object versions"
    override def resource: String = Resource.forBucket(bucketName)
    override def runOnce: Route = {
      complete("")
    }
  }
}
