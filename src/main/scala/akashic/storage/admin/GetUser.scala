package akashic.storage.admin

import scala.xml.NodeSeq

object GetUser {
  case class Result(xml: NodeSeq)
  def run(users: UserTable, id: String): Result = {
    val user = users.getUser(id)
    if (user.isEmpty) Error.failWith(Error.NotFound())
    Result(User.toXML(user.get))
  }
}
