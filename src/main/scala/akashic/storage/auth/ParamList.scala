package akashic.storage.auth

import akka.http.scaladsl.model.HttpRequest

object ParamList {
  case class t(unwrap: Seq[(String, String)]) {
    def find(k: String) = unwrap.find(_._1.toLowerCase == k.toLowerCase).map(_._2)
  }
  val empty = t(Seq())
  def fromRequest(req: HttpRequest): t = {
    t(req.uri.query())
  }
}
