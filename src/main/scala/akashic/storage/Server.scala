package akashic.storage

import akashic.storage.admin._
import akashic.storage.backend.{BALFactory, NodePath}
import akashic.storage.caching.CacheMaps
import akashic.storage.patch.{Astral, Tree}
import akashic.storage.service._
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._
import akka.http.scaladsl.model.{HttpEntity, StatusCode, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive0, ExceptionHandler, Route}
import akka.util.ByteString

import scala.concurrent.Future

case class Server(config: ServerConfig, cleanup: Boolean) {
  fs = new BALFactory(config.rawConfig.getConfig("backend")).build
  private val root = NodePath(null, "/", "", Some(fs.getRoot))

  if (cleanup) {
    root.cleanDirectory
    logger.info("mountpoint is cleaned up")
  }

  if (root.listDir.isEmpty) {
    logger.info("initialize mountpoint")
    root("tree").makeDirectory
    root("admin").makeDirectory
    root("astral").makeDirectory
  }

  private val initialized =
    root("tree").exists &&
    root("admin").exists &&
    root("astral").exists
  require(initialized)
  logger.info("mountpoint is initialized")

  val tree = Tree(root("tree"))
  val users = UserDB(root("admin"))
  val astral = Astral(root("astral"))
  val cacheMaps = CacheMaps(config)

  private val adminRoute =
    Add.route ~
    List.route ~
    Get.route ~
    Update.route

  private val adminErrHandler = ExceptionHandler {
    case admin.Error.Exception(e) =>
      val (code, message) = admin.Error.interpret(e)
      val status = StatusCode.int2StatusCode(code)
      complete(status, ResponseHeaderList.builder.build, message)
    case _ =>
      complete(StatusCodes.InternalServerError, HttpEntity.Empty)
  }

  // I couldn't place this in service package
  // My guess is evaluation matters for the null pointer issue
  private val serviceRoute =
    ListMultipartUploads.route ~    // GET    /bucketName?uploads
    GetBucketAcl.route ~            // GET    /bucketName?acl
    GetBucketLocation.route ~       // GET    /bucketName?location
    GetBucketVersioning.route ~     // GET    /bucketName?versioning
    GetBucketObjectVersions.route ~ // GET    /bucketName?versions
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
    DeleteMultipleObjects.route ~   // POST   /bucketName?delete
    InitiateMultipartUpload.route ~ // POST   /bucketName/keyName?uploads
    CompleteMultipartUpload.route ~ // POST   /bucketName/keyName?uploadId=***
    DeleteBucket.route ~            // DELETE /bucketName
    AbortMultipartUpload.route ~    // DELETE /bucketName/keyName?uploadId=***
    DeleteObject.route              // DELETE /bucketName/keyName

  private val serviceErrHandler = ExceptionHandler {
    case service.Error.Exception(context, e) =>
      val withMessage = service.Error.withMessage(e)
      val xml = service.Error.mkXML(withMessage, context.resource, context.requestId)
      val status = StatusCode.int2StatusCode(withMessage.httpCode)
      complete(status, ResponseHeaderList.builder.build, xml)
    case _ =>
      complete(StatusCodes.InternalServerError, HttpEntity.Empty)
  }

  private val apiRoute =
    handleExceptions(adminErrHandler) { admin.apiLogger(adminRoute) } ~
    handleExceptions(serviceErrHandler) { service.apiLogger(serviceRoute) }

  private val ignoreEntity: Directive0 = entity(as[ByteString]).tflatMap(_ => pass)
  private val unmatchRoute =
    // We need to extract entity to consume the payload
    // otherwise client never knows the end of connection.
    ignoreEntity { complete(StatusCodes.BadRequest, HttpEntity.Empty) }

  private val route =
    apiRoute ~
    unmatchRoute

  def address = s"${config.ip}:${config.port}"

  private var binding: Future[Http.ServerBinding] = _

  def start: Future[ServerBinding] = {
    logger.info("start server")
    binding = Http().bindAndHandle(
      handler = Route.handlerFlow(route),
      interface = config.ip,
      port = config.port)
    binding
  }

  def stop: Future[Unit] = {
    logger.info("stop server")
    binding.flatMap(_.unbind)
  }
}
