package akashic.storage.service

trait Task[T] {
  def runOnce: T
  def run: T = {
    try {
      runOnce
    } catch {
      case e: Error.Exception => throw e
      case _: Throwable => runOnce
    }
  }
}
