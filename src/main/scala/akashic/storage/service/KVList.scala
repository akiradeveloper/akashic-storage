package akashic.storage.service

import scala.collection.mutable

object KVList {
  case class t(unwrap: Seq[(String, String)]) {
    def find(k: String) = unwrap.find(_._1 == k).map(_._2)
  }
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
}
