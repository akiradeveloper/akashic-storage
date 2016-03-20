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

  system = ActorSystem("akashic-storage")
  mat = ActorMaterializer()
  ec = system.dispatcher
  sys.addShutdownHook(system.shutdown)

  var server: Server = _
  val logger = Logging.getLogger(system, "akashic.storage")
}
