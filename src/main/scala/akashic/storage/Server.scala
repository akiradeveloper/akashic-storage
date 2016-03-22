package akashic.storage

import java.nio.file.{Paths, Files}

import akashic.storage.admin._
import akashic.storage.caching.CacheMaps
import akashic.storage.service._
import akashic.storage.patch.{Astral, Tree}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpEntity, StatusCodes, StatusCode}
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.{Directive0, ExceptionHandler, Route}
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._
import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging
import org.apache.commons.io.FileUtils
import scala.concurrent.Future

case class Server(config: ServerConfig, cleanup: Boolean) {
  require(Files.exists(config.mountpoint))

  if (cleanup) {
    FileUtils.cleanDirectory(config.mountpoint.toFile)
    logger.info("mountpoint is cleaned up")
  }

  if (files.children(config.mountpoint).isEmpty) {
    logger.info("initialize mountpoint")
    Files.createDirectory(config.mountpoint.resolve("tree"))
    Files.createDirectory(config.mountpoint.resolve("admin"))
    Files.createDirectory(config.mountpoint.resolve("astral"))
  }

  val initialized =
    Files.exists(config.mountpoint.resolve("tree")) &&
    Files.exists(config.mountpoint.resolve("admin")) &&
    Files.exists(config.mountpoint.resolve("astral"))
  require(initialized)
  logger.info("mountpoint is initialized")

  val tree = Tree(config.mountpoint.resolve("tree"))
  val users = UserDB(config.mountpoint.resolve("admin"))
  val astral = Astral(config.mountpoint.resolve("astral"))
  val cacheMaps = CacheMaps(config)

  val adminRoute =
    Add.route ~
    List.route ~
    Get.route ~
    Update.route

  val adminErrHandler = ExceptionHandler {
    case admin.Error.Exception(e) =>
      val (code, message) = admin.Error.interpret(e)
      val status = StatusCode.int2StatusCode(code)
      complete(status, ResponseHeaderList.builder.build, message)
    case _ =>
      complete(StatusCodes.InternalServerError, HttpEntity.Empty)
  }

  // I couldn't place this in service package
  // My guess is evaluation matters for the null pointer issue
  val serviceRoute =
    GetBucketAcl.route ~            // GET    /bucketName?acl
    GetBucketLocation.route ~       // GET    /bucketName?location
    GetBucket.route ~               // GET    /bucketName
    ListParts.route ~               // GET    /bucketName/keyName?uploadId=***
    GetObjectAcl.route ~            // GET    /bucketName/keyName&acl
    GetObject.route ~               // GET    /bucketName/keyName
    GetService.route ~              // GET    /
    HeadBucket.route ~              // HEAD   /bucketName
    HeadObject.route ~              // HEAD   /bucketName/keyName
    PutBucketAcl.route ~            // PUT    /bucketName?acl
    PutBucket.route ~               // PUT    /bucketName
    UploadPart.route ~              // PUT    /bucketName/keyName?uploadId=***?partNumber=***
    PutObjectAcl.route ~            // PUT    /bucketName/keyName?acl
    PutObject.route ~               // PUT    /bucketName/keyName
    DeleteBucket.route ~            // DELETE /bucketName
    AbortMultipartUpload.route ~    // DELETE /bucketName/keyName?uploadId=***
    DeleteObject.route ~            // DELETE /bucketName/keyName
    // PostObject.route ~              // POST   /bucketName
    DeleteMultipleObjects.route ~   // POST   /bucketName
    InitiateMultipartUpload.route ~ // POST   /bucketName/keyName?uploads
    CompleteMultipartUpload.route   // POST   /bucketName/keyName?uploadId=***

  val serviceErrHandler = ExceptionHandler {
    case service.Error.Exception(context, e) =>
      val withMessage = service.Error.withMessage(e)
      val xml = service.Error.mkXML(withMessage, context.resource, context.requestId)
      val status = StatusCode.int2StatusCode(withMessage.httpCode)
      complete(status, ResponseHeaderList.builder.build, xml)
    case _ =>
      complete(StatusCodes.InternalServerError, HttpEntity.Empty)
  }

  val apiRoute =
    handleExceptions(adminErrHandler) { admin.apiLogger(adminRoute) } ~
    handleExceptions(serviceErrHandler) { scala.concurrent.blocking(service.apiLogger(serviceRoute)) }

  val ignoreEntity: Directive0 = entity(as[ByteString]).tflatMap(_ => pass)
  val unmatchRoute =
    // We need to extract entity to consume the payload
    // otherwise client never knows the end of connection.
    ignoreEntity { complete(StatusCodes.BadRequest, HttpEntity.Empty) }

  val route =
    apiRoute ~
    unmatchRoute

  def address = s"${config.ip}:${config.port}"

  var binding: Future[Http.ServerBinding] = _

  def start = {
    logger.info("start server")
    binding = Http().bindAndHandle(
      handler = Route.handlerFlow(route),
      interface = config.ip,
      port = config.port)
    binding
  }

  def stop = {
    logger.info("stop server")
    binding.flatMap(_.unbind)
  }
}
