package akasha.api

import java.nio.file.{Files, Path}

import org.apache.commons.io.IOUtils

import scala.util.Try
import scala.xml.NodeSeq
import scala.pickling.Defaults._
import scala.pickling.binary._

object Cors {
  case class t(rules: Seq[Rule]) {
    def toBytes: Array[Byte] = this.pickle.value
  }
  def fromBytes(bytes: Array[Byte]): t = BinaryPickle(bytes).unpickle[t]

  trait WildCardSupport {
    val unwrap: String
    def tryMatch[T <: WildCardSupport](a: T): Boolean = {
      if (unwrap == "*") { // FIXME support wildcard in the middle way or use regex
        true
      } else {
        a.unwrap == unwrap
      }
    }
  }
  case class AllowedOrigin(unwrap: String) extends WildCardSupport
  case class AllowedMethod(unwrap: String) extends WildCardSupport
  case class AllowedHeader(unwrap: String) extends WildCardSupport

  case class MaxAgeSeconds(unwrap: Int)
  case class ExposeHeader(unwrap: String)

  case class Request(origin: AllowedOrigin,
                     requestMethod: AllowedMethod,
                     requestHeaders: Seq[AllowedHeader])

  case class Response(origin: AllowedOrigin,
                      maxAgeSeconds: Option[MaxAgeSeconds],
                      allowedMethods: AllowedMethod,
                      allowedHeaders: Seq[AllowedHeader],
                      exposeHeaders: Seq[ExposeHeader])

  // a is a subset of b
  def subsetOf[T <: WildCardSupport](a: Seq[T], b: Seq[T]): Boolean = {
    a.map { a => b.exists(_.tryMatch(a)) }.forall(_ == true)
  }

  case class Rule(origins: Seq[AllowedOrigin],
                  allowedMethods: Seq[AllowedMethod],
                  allowedHeaders: Seq[AllowedHeader],
                  maxAgeSeconds: Option[MaxAgeSeconds],
                  exposeHeaders: Seq[ExposeHeader]) {

    def tryMatch(req: Request): Option[Response] = {
      Try {
        // Not sure this implementation is correct but looks natural to me.
        val resOrigin = if (origins.exists(_.tryMatch(req.origin))) { Some(req.origin) } else { None }
        val resAllowedMethods = if (allowedMethods.exists(_.tryMatch(req.requestMethod))) { Some(req.requestMethod) } else { None }
        val resAllowedHeaders = if (subsetOf(req.requestHeaders, allowedHeaders)) { Some(req.requestHeaders) } else { None }
        Response(resOrigin.get, maxAgeSeconds, resAllowedMethods.get, resAllowedHeaders.get, exposeHeaders)
      }.toOption
    }
  }

  // FIXME return which rule was hit to return exposedHeaders
  def tryMatch(rules: Seq[Rule], req: Request): Option[(Rule, Response)] = {
    rules.map { rule => rule.tryMatch(req).map {a => (rule, a)} }
      .find(_.isDefined).flatten
  }

  def parseXML(xml: NodeSeq): Seq[Rule] = {
    (xml \ "CORSRule").map { rule =>
      Rule(
        (rule \ "AllowedOrigin").map { a => AllowedOrigin(a.text) },
        (rule \ "AllowedMethod").map { a => AllowedMethod(a.text) },
        (rule \ "AllowedHeader").map { a => AllowedHeader(a.text) },
        (rule \ "MaxAgeSeconds").headOption.map { a => MaxAgeSeconds(a.text.toInt) },
        (rule \ "ExposeHeader").map { a => ExposeHeader(a.text) }
      )
    }
  }
}
