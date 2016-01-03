package akasha.admin

object UpdateUser {
  case class Result()
  def run(users: UserTable, id: String, body: String): Result = {
    val xml = XML.loadString(body)
    val user = users.getUser(id)
    user.isDefined.orFailWith(Error.AccountProblem())
    val newUser = user.get.modifyWith(xml)
    users.updateUser(id, newUser)
    Result()
  }
}
