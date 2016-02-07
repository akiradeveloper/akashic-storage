package akashic.storage

import java.nio.file.Files

import akashic.storage.admin._
import akashic.storage.service._
import akashic.storage.patch.{Astral, Tree}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._

case class Server(config: ServerConfig) {
  Files.createDirectory(config.mountpoint.resolve("tree"))
  val tree = Tree(config.mountpoint.resolve("tree"))

  Files.createDirectory(config.mountpoint.resolve("admin"))
  val users = UserTable(config.mountpoint.resolve("admin"))

  Files.createDirectory(config.mountpoint.resolve("astral"))
  val astral = Astral(config.mountpoint.resolve("astral"))

  val adminRoute =
    MakeUser.route

  // I couldn't place this in service package
  // My guess is evaluation matters for the null pointer issue
  val serviceRoute =
    GetBucket.route ~
      GetObject.route ~
      GetService.route ~
      PutBucket.route ~
      UploadPart.route ~
      PutObject.route ~
      DeleteBucket.route ~
      DeleteObject.route ~
      InitiateMultipartUpload.route

  val route =
    adminRoute ~
    serviceRoute

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
