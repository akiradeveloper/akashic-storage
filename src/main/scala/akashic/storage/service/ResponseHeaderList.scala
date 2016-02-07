package akashic.storage.service

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.headers.RawHeader

import scala.collection.{mutable, immutable}

object ResponseHeaderList {
  def builder = Builder()
  case class Builder() {
    private val l = mutable.ListBuffer[HttpHeader]()
    def withHeader(k: String, v: String): this.type = {
      withHeader(RawHeader(k, v))
    }
    def withHeader(k: String, ov: Option[String]): this.type = {
      withHeader(ov.map(v => RawHeader(k, v)))
    }
    def withHeader(header: HttpHeader): this.type = {
      l += header
      this
    }
    def withHeader(headerOpt: Option[HttpHeader]): this.type = {
      headerOpt match {
        case Some(header) => l += header
        case None =>
      }
      this
    }
    def build: immutable.Seq[HttpHeader] = l.toList
  }
}
