package akasha.admin

import scala.xml.NodeSeq
import io.finch._

object GetUser {
  case class Result(xml: NodeSeq)
  def run(users: UserTable, id: String): Result = {
    val user = users.getUser(id)
    if (user.isEmpty) Error.failWith(Error.NotFound())
    Result(User.toXML(user.get))
  }
}
