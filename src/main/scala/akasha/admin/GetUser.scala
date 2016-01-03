package akasha.admin

object GetUser {
  case class Result(xml: NodeSeq)
  def run(users: UserTable, id: String): Result = {
    val user = users.getUser(id)
    // FIXME error type
    user.isDefined.orFailWith(Error.AccountProblem())
    Ok(User.toXML(user.get))
  }
}
