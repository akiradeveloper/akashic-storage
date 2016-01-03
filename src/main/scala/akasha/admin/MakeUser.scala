package akasha.admin

object MakeUser {
  case class Result(xml: NodeSeq)
  def run(users: UserTable): Result {
    val newUser = users.mkUser
    Result(User.toXML(newUser))
  }
}
