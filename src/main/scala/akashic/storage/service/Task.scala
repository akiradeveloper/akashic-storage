package akashic.storage.service

trait Task[T] extends Runnable[T] {
  def runOnce: T
  def run: T = {
    var retry = 0
    val result = try {
      runOnce
    } catch {
      case e: Error.Exception => throw e
      case _: Throwable =>
        retry += 1
        runOnce
    }
    result
  }
}
