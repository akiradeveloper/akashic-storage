package akashic.storage.service

import akka.http.scaladsl.server.Route

trait Task extends Runnable {
  def runOnce: Route
  def run = {
    var retry = 0
    val result = try {
      runOnce
    } catch {
      case e: Error.Exception => throw e
      case _: Throwable =>
        retry += 1
        runOnce
    }
    result
  }
}
