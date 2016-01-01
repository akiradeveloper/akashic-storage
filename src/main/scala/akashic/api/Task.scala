package akashic.api

trait Task[T] {
  def doRun: T
  def run: T = {
    try {
      doRun
    } catch {
      case akashic.Err(e) => throw e
      case _ => run
    }
  }
}
