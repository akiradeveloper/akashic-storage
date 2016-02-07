package akashic.storage

import akka.http.scaladsl.model.{ContentTypes, ContentType, HttpRequest}

import scala.collection.mutable

object HeaderList {
  case class t(unwrap: Seq[(String, String)]) {
    def find(k: String) = unwrap.find(_._1.toLowerCase == k.toLowerCase).map(_._2)
  }
  val empty = t(Seq())
  def builder: Builder = Builder()
  case class Builder() {
    private val l = mutable.ListBuffer[(String, String)]()
    def append(k: String, v: String): this.type = {
      l += k -> v
      this
    }
    def appendOpt(k: String, ov: Option[String]): this.type = {
      ov match {
        case Some(v) => l += k -> v
        case None =>
      }
      this
    }
    def build = t(l)
  }
  def fromRequest(req: HttpRequest): t = {
    t(req.headers.map(a => (a.name, a.value)))
  }
}
