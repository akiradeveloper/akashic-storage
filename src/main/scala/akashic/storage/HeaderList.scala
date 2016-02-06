package akashic.storage

import akka.http.scaladsl.model.HttpRequest

import scala.collection.mutable

object HeaderList {
  case class t(unwrap: Seq[(String, String)]) {
    def find(k: String) = unwrap.find(_._1.toLowerCase == k.toLowerCase).map(_._2)
  }
  val empty = t(Seq())
  def builder: Builder = Builder()
  case class Builder() {
    private val l = mutable.ListBuffer[(String, String)]()
    def append(pair: (String, String)): this.type = {
      l += pair
      this
    }
    def appendOpt(pair: (String, Option[String])): this.type = {
      val (k, v) = pair
      if (v.isDefined) { l += k -> v.get }
      this
    }
    def build = t(l)
  }
  def fromRequest(req: HttpRequest): t = {
    t(req.headers.map(a => (a.name, a.value)) ++ Seq(("Content-Type", req.entity.getContentType.mediaType.toString)))
  }
}
