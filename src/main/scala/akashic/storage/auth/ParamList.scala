package akashic.storage.auth

import com.twitter.finagle.http.Request

object ParamList {
  case class t(unwrap: Seq[(String, String)]) {
    def find(k: String) = unwrap.find(_._1 == k).map(_._2)
  }
  def fromRequest(req: Request): t = {
    t(req.params.iterator.toSeq)
  }
}
