package akashic.storage.service

import java.lang.{IllegalArgumentException, RuntimeException}

import akashic.storage.server
import akka.http.scaladsl.model.{StatusCodes, HttpRequest}
import akka.http.scaladsl.server.Directives._
import scala.util.{Failure, Success, Try}
import scala.xml.{NodeSeq, XML}
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._

object DeleteMultipleObjects {
  val matcher =
    post &
    extractBucket &
    entity(as[String]) &
    extractRequest

  val route = matcher.as(t)(_.run)

  case class t(bucketName: String,
               xmlString: String,
               req: HttpRequest) extends AuthorizedAPI {
    override def name = "DELETE Multiple Objects"
    override def resource = Resource.forBucket(bucketName)
    override def runOnce = {
      case class ObjectToDelete(keyName: String, versionId: Option[Int])
      case class Input(quite: Boolean, objects: Seq[ObjectToDelete])
      val input = Try {
        val xml = XML.loadString(xmlString)
        // TODO Quite mode
        val objects = (xml \ "Object").map { o =>
          val key = (o \ "Key").text
          val versionId = (o \ "VersionId") match {
            case NodeSeq.Empty => None
            case a => Some(a.text.toInt)
          }
          ObjectToDelete(encodeKeyName(key), versionId)
        }
        Input(false, objects)
      } match {
        case Success(a) => a
        case Failure(_) => failWith(Error.MalformedXML())
      }
      val bucket = findBucket(server.tree, bucketName)

      sealed trait DeleteResult { def toXML: NodeSeq }
      case class DeletedResult(keyName: String,
                               // VersionId for the versioned object in the case of a versioned delete.
                               versionId: Option[Int],
                               // DeleteMarker:
                               // If a specific delete request either creates or deletes a delete marker,
                               // Amazon S3 returns this element in the response with a value of true.
                               // DeleteMarkerVersionId:
                               // If the specific delete request in the Multi-Object Delete either creates or deletes a delete marker,
                               // Amazon S3 returns this element in response with the version ID of the delete marker.
                               deleteMarker: Option[Int]) extends DeleteResult {
        override def toXML: NodeSeq = {
          <Deleted>
            <Key>{decodeKeyName(keyName)}</Key>
            { versionId match { case Some(a) => <VersionId>{a}</VersionId>; case None => NodeSeq.Empty } }
            { deleteMarker match { case Some(a) => <DeleteMarker>true</DeleteMarker>; case None => NodeSeq.Empty } }
            { deleteMarker match { case Some(a) => <DeleteMarkerVersionId>{a}</DeleteMarkerVersionId>; case None => NodeSeq.Empty } }
          </Deleted>
        }
      }
      case class FailedResult(keyName: String,
                              versionId: Option[Int] ,
                              code: String, // AccessDenied or InternalError
                              message: String) extends DeleteResult {
        override def toXML: NodeSeq = {
          <Error>
            <Key>{decodeKeyName(keyName)}</Key>
            { versionId match { case Some(a) => <VersionId>{a}</VersionId>; case None => NodeSeq.Empty } }
            <Code>{code}</Code>
            <Message>{message}</Message>
          </Error>
        }
      }

      case class DeleteError(accessDenied: Boolean) extends RuntimeException
      val results: Seq[DeleteResult] = input.objects.map { o =>
        import o._
        try {
          val key = bucket.findKey(keyName) match {
            case Some(a) => a
            case None => throw DeleteError(accessDenied = true)
          }
          val deleteResult = DeleteObject.deleteObject(bucket, key, versionId)
          DeletedResult(keyName, versionId, deleteResult)
        } catch {
          case DeleteError(true) => FailedResult(keyName, versionId, "AccessDenied", "Access Denied")
          case _: Throwable => FailedResult(keyName, versionId, "InternalError", "Internal Error")
        }
      }

      val headers = ResponseHeaderList.builder
        .withHeader(X_AMZ_REQUEST_ID, requestId)
        .build

      complete(StatusCodes.OK, headers,
        <DeleteResult>
          { for (result <- results) yield result.toXML }
        </DeleteResult>)
    }
  }
}
