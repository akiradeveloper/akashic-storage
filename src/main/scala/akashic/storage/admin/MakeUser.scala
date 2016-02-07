package akashic.storage.admin

import scala.xml.NodeSeq
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._
import akashic.storage.server

object MakeUser {
  val matcher =
    post &
    path("admin" / "user")
  val route = matcher {
    val result = run(server.users)
    complete(result.xml)
  }
  case class Result(xml: NodeSeq)
  def run(users: UserTable): Result = {
    val newUser = users.mkUser
    Result(User.toXML(newUser))
  }
}
