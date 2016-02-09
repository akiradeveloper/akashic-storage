package akashic.storage.service
import akka.http.scaladsl.model.{StatusCodes, HttpRequest}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

object DeleteMultipleObjects {
  val matcher =
    post &
    extractBucket &
    entity(as[String]) &
    extractRequest

  val route = matcher.as(t)(_.run)

  case class t(bucketName: String,
               xmlString: String,
               req: HttpRequest) extends API {
    override def name = "DELETE Multiple Objects"
    override def resource = Resource.forBucket(bucketName)
    override def runOnce = {
      complete("")
    }
  }
}
