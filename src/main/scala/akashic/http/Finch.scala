package akashic.http

import cats.Eval
import com.twitter.util.{Await, Future}
import io.finch.internal.Mapper

import scala.xml.NodeSeq

object FinchExtensions {

  implicit val encodeXML: EncodeResponse[NodeSeq] = EncodeResponse.fromString("application/xml")(a => a.toString)

  implicit class MonadicEndpoint[A](self: Endpoint[A]) {
    def flatMap[B](fn: A => Endpoint[B]): Endpoint[B] = new Endpoint[B] {
      override def apply(input: Input): Option[(Input, Eval[Future[Output[B]]])] = for {
        (r1, ao) <- self(input)
        (r2, bo) <- fn(Await.result(ao.value).value)(r1)
      } yield (r2, bo)
    }
  }

  implicit def mapperFromEndpointFunction[A, B](f: A => Endpoint[B]): Mapper.Aux[A, B] = new Mapper[A] {
    type Out = B
    def apply(r: Endpoint[A]): Endpoint[Out] = r.flatMap(f)
  }
}
