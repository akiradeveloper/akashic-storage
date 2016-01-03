package akasha.api

trait Task[T] {
  def doRun: T
  def run: T = {
    try {
      doRun
    } catch {
      case akasha.Error(e) => throw e
      case _ => run
    }
  }
}
