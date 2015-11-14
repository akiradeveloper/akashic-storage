import com.twitter.finagle.Http
import com.twitter.finagle.http.Response
import com.twitter.io.Buf.Utf8
import com.twitter.util.{Future, Await}
import io.finch._
import io.finch.request._
import io.finch.response.EncodeResponse
import com.twitter.io.Buf
import shapeless.HNil

object FinchPlay extends App {

  implicit val failureEncoder: EncodeResponse[Map[String, String]] =
    EncodeResponse("text/plain")(map =>
      Buf.Utf8(map.toSeq.map(kv => "\"" + kv._1 + "\":\"" + kv._2 + "\"").mkString(", "))
    )

  case class Hello(unwrap: String)
  implicit val helloEncoder = new EncodeResponse[Hello] {
    override def apply(rep: Hello): Buf = Utf8(rep.unwrap)
    override def contentType: String = "application/xml" // should fail on client side
  }

  case class GoodBye(unwrap: String)
  implicit val goodByeEncoder = new EncodeResponse[GoodBye] {
    override def apply(rep: GoodBye): Buf = Utf8(rep.unwrap)
    override def contentType: String = "text/plain"
  }

  val service =
    // get("a") ? param("b") isn't correct
    get("a" ? param("b")) { b: String => Ok(b) } :+:
    get("hello") { Forbidden(("a", "b"), ("c", "d")): Output[Unit] } :+:
    get("goodbye") { Ok(GoodBye("GoodBye")) } :+:
    // raw finagle.http.response
    get("raw").map { _ =>
      val r = Response()
      r.contentString = "raw"
      r
    }
  Await.ready(Http.server.serve(":8080", service.toService))
}
