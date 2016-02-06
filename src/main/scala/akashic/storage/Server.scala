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

  val route =
    GetBucket.route ~
    GetService.route ~
    PutBucket.route ~
    PutObject.route

  def address = s"${config.ip}:${config.port}"

  implicit var system: ActorSystem = _
  implicit var mat: ActorMaterializer = _

  def start = {
    system = ActorSystem()
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
