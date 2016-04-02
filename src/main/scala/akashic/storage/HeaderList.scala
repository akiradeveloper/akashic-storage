package akashic.storage

import akka.http.scaladsl.model.HttpRequest

import scala.collection.mutable

case class HeaderList(unwrap: Seq[(String, String)]) {
  def find(k: String) = unwrap.find(_._1.toLowerCase == k.toLowerCase).map(_._2)
}

object HeaderList {
  type t = HeaderList
  val empty = HeaderList(Seq())
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
    def build = HeaderList(l)
  }
  def fromRequest(req: HttpRequest): t = {
    HeaderList(req.headers.map(a => (a.name, a.value)))
  }
}
