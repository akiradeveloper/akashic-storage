package akashic.storage

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.DebuggingDirectives
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

package object admin {
  val logger = Logger(LoggerFactory.getLogger("akashic.storage.admin"))

  val apiLogger = withLog(logger).tflatMap(_ => DebuggingDirectives.logRequestResult(""))
  val authenticate = Auth.authenticate
}
