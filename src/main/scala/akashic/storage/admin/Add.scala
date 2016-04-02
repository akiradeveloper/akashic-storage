package akashic.storage.admin

import akashic.storage.server
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._
import akka.http.scaladsl.server.Directives._

import scala.xml.NodeSeq

object Add {
  val matcher =
    post &
    path("admin" / "user")
  val route = matcher {
    authenticate {
      val result = run(server.users)
      complete(result.xml)
    }
  }
  case class Result(xml: NodeSeq)
  def run(users: UserDB): Result = {
    val newUser = users.mkUser
    Result(User.toXML(newUser))
  }
}
