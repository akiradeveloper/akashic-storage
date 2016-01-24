package akashic.storage

import com.twitter.finagle.http.Request

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
    def appendOpt(k: String, v: Option[String]): this.type = {
      if (v.isDefined) { l += k -> v.get }
      this
    }
    def build = t(l)
  }
  def fromRequest(req: Request): t = {
    t(req.headerMap.iterator.toSeq)
  }
}