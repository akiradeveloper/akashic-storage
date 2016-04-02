package akashic.storage.admin

import akashic.storage.server
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._
import akka.http.scaladsl.server.Directives._

import scala.xml.NodeSeq

object Get {
  val matcher =
    get &
    path("admin" / "user" / Segment)

  val route = matcher { id: String =>
    authenticate {
      val result = run(server.users, id)
      complete(result.xml)
    }
  }

  case class Result(xml: NodeSeq)
  def run(users: UserDB, id: String): Result = {
    val user = users.find(id) match {
      case Some(a) => a
      case None => Error.failWith(Error.NotFound())
    }
    Result(User.toXML(user))
  }
}
