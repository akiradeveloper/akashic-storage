package akashic.storage

import com.twitter.finagle.http.Request

package object auth {
  def authorize(resource: String, req: Request): Option[String] =
    Seq(V2.authorize(resource, req), V2Presigned.authorize(resource, req)).find(_.isDefined).get
}
