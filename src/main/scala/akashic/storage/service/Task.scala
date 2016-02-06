package akashic.storage.service

trait Task[T] extends RequestIdAllocable with Authorizable {
  def name: String
  def runOnce: T
  def run: T = {
    println(s"-> ${name}")
    val start = System.currentTimeMillis
    allocRequestId
    authorize
    var retry = 0
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
