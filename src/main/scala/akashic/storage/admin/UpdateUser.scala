package akashic.storage.admin

import akka.http.scaladsl.model.{HttpEntity, StatusCodes}

import scala.util.{Failure, Success, Try}
import scala.xml.XML
import akka.http.scaladsl.server.Directives._
import akashic.storage.server

object UpdateUser {
  val matcher =
    put &
    path("admin" / "user" / Segment) &
    entity(as[String])

  val route = matcher { (id: String, xmlString: String) =>
    val status = Try {
      run(server.users, id, xmlString)
    } match {
      case Success(_) => StatusCodes.OK
      case Failure(_) => StatusCodes.ServerError // FIXME
    }
    complete(HttpEntity.Empty)
  }

  case class Result()
  def run(users: UserTable, id: String, body: String): Result = {
    val xml = XML.loadString(body)
    val user = users.getUser(id) match {
      case Some(a) => a
      case None => Error.failWith(Error.NotFound())
    }
    val newUser = user.modifyWith(xml)
    users.updateUser(id, newUser)
    Result()
  }
}
