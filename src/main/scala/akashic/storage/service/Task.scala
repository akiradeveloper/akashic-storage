package akashic.storage.service

trait Task[T] extends CallerIdAssertable {
  def name: String
  def runOnce: T
  def run: T = {
    checkCallerId

    val start = System.currentTimeMillis
    var retry = 0
    println(s"-> ${name}")
    val a = try {
      runOnce
    } catch {
      case e: Error.Exception => throw e
      case _: Throwable =>
        retry += 1
        runOnce
    }
    val end = System.currentTimeMillis
    println(s"<- ${name} ${end-start}[ms] (${retry} retry)")
    a
  }
}
