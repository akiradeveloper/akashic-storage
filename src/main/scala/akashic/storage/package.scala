package akashic

import akashic.storage.service._
import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.ActorMaterializer
import org.slf4j.{LoggerFactory, Logger}

import scala.concurrent.ExecutionContext

package object storage {
  implicit var system: ActorSystem = _
  implicit var mat: ActorMaterializer = _
  implicit var ec: ExecutionContext = _

  var server: Server = _
  val logger = LoggerFactory.getLogger(getClass)
}
