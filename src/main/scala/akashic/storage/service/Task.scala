package akashic.storage.service

trait Task[T] {
  def name: String
  def runOnce: T
  def run: T = {
    println(s"-> ${name}")
    val a = try {
      runOnce
    } catch {
      case e: Error.Exception => throw e
      case _: Throwable => runOnce
    }
    println(s"<- ${name}")
    a
  }
}
