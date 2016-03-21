package akashic.storage

import akka.event.Logging
import akka.http.scaladsl.server.directives.{LoggingMagnet, DebuggingDirectives}
import com.typesafe.scalalogging.{Logger, StrictLogging}
import org.slf4j.LoggerFactory
import akka.http.scaladsl.server.Directives._

package object admin {
  val logger = Logger(LoggerFactory.getLogger("akashic.storage.admin"))

  val apiLogger = withLog(logger).tflatMap(_ => DebuggingDirectives.logRequestResult(""))
  val authenticate = Auth.authenticate
}
