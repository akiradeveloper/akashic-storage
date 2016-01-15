package akashic.storage.admin

import scala.xml.XML

object UpdateUser {
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
