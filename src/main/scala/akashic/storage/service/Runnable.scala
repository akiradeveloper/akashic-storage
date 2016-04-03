package akashic.storage.service

import akka.http.scaladsl.server.Route

trait Runnable {
  def run: Route
}
