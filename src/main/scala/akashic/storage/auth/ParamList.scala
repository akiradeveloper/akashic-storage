package akashic.storage.auth

import akka.http.scaladsl.model.HttpRequest

case class ParamList(unwrap: Seq[(String, String)]) {
  def find(k: String) = unwrap.find(_._1.toLowerCase == k.toLowerCase).map(_._2)
}

object ParamList {
  val empty = ParamList(Seq())
  def fromRequest(req: HttpRequest) = {
    ParamList(req.uri.query())
  }
}
