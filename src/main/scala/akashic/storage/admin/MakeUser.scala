package akashic.storage.admin

import scala.xml.NodeSeq

object MakeUser {
  case class Result(xml: NodeSeq)
  def run(users: UserTable): Result = {
    val newUser = users.mkUser
    Result(User.toXML(newUser))
  }
}
