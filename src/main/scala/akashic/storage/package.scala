package akashic

import org.slf4j.{LoggerFactory, Logger}

package object storage {
  var server: Server = _
  val logger = LoggerFactory.getLogger(getClass)
}
