package akasha.admin

object Error {
  trait t
  case class Exception(e: t) extends RuntimeException
  case class NotFound() extends t
  def interpret(e: t): (Int, String) = e match {
    case NotFound() => (404, "account not found")
    case _ => (500, "unknown error")
  }
  def failWith(e: t) = throw Exception(e)
}
