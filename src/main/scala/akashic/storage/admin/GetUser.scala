package akashic.storage.admin

import scala.xml.NodeSeq
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._
import akashic.storage.server

object GetUser {
  val matcher =
    get &
    path("admin" / "user" / Segment)

  val route = matcher { id: String =>
    val result = run(server.users, id)
    complete(result.xml)
  }

  case class Result(xml: NodeSeq)
  def run(users: UserTable, id: String): Result = {
    val user = users.getUser(id) match {
      case Some(a) => a
      case None => Error.failWith(Error.NotFound())
    }
    Result(User.toXML(user))
  }
}
