package akashic.storage

import java.nio.file.Files

import akashic.storage.admin._
import akashic.storage.service._
import akashic.storage.patch.{Astral, Tree}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._

case class Server(config: ServerConfig) {
  Files.createDirectory(config.mountpoint.resolve("tree"))
  val tree = Tree(config.mountpoint.resolve("tree"))

  Files.createDirectory(config.mountpoint.resolve("admin"))
  val users = UserTable(config.mountpoint.resolve("admin"))

  Files.createDirectory(config.mountpoint.resolve("astral"))
  val astral = Astral(config.mountpoint.resolve("astral"))

  val adminRoute =
    MakeUser.route ~
    GetUser.route ~
    UpdateUser.route

  // I couldn't place this in service package
  // My guess is evaluation matters for the null pointer issue
  val serviceRoute =
    GetBucket.route ~               // GET    /bucketName
    ListParts.route ~               // GET    /bucketName/keyname?uploadId=***
    GetObject.route ~               // GET    /bucketName/keyName
    GetService.route ~              // GET    /
    HeadBucket.route ~              // HEAD   /bucketName
    HeadObject.route ~              // HEAD   /bucketName/keyName
    PutBucket.route ~               // PUT    /bucketName
    UploadPart.route ~              // PUT    /bucketName/keyName?uploadId=***?partNumber=***
    PutObject.route ~               // PUT    /bucketName/keyName
    DeleteBucket.route ~            // DELETE /bucketName
    AbortMultipartUpload.route ~    // DELETE /bucketName/keyName?uploadId=***
    DeleteObject.route ~            // DELETE /bucketName/keyName
    InitiateMultipartUpload.route ~ // POST   /bucketName/keyName?uploads
    CompleteMultipartUpload.route   // POST   /bucketName/keyName?uploadId=***

  val apiRoute =
    adminRoute ~
    serviceRoute

  val errHandler = ExceptionHandler {
    case service.Error.Exception(context, e) =>
      val withMessage = service.Error.withMessage(e)
      val xml = service.Error.mkXML(withMessage, context.resource, context.requestId)
      val status = StatusCode.int2StatusCode(withMessage.httpCode)
      complete(status, ResponseHeaderList.builder.build, xml)
    case admin.Error.Exception(e) =>
      val (code, message) = admin.Error.interpret(e)
      val status = StatusCode.int2StatusCode(code)
      complete(status, ResponseHeaderList.builder.build, message)
  }

  val route =
    handleExceptions(errHandler) { apiRoute }

  def address = s"${config.ip}:${config.port}"

  implicit var system: ActorSystem = _
  implicit var mat: ActorMaterializer = _

  def start = {
    system = ActorSystem("akashic-storage")
    mat = ActorMaterializer()
    Http().bindAndHandle(
      handler = Route.handlerFlow(route),
      interface = config.ip,
      port = config.port)
  }

  def stop: Unit = {
    system.shutdown
    system.awaitTermination
  }
}
