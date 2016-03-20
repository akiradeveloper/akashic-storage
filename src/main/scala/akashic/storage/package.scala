package akashic

import akashic.storage.service._
import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.ActorMaterializer
import org.slf4j.{LoggerFactory, Logger}

import scala.concurrent.ExecutionContext

package object storage {
  implicit val system: ActorSystem = ActorSystem("akashic-storage")
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher
  sys.addShutdownHook(system.shutdown)

  var server: Server = _
  val logger = Logging.getLogger(system, "akashic.storage")
}
