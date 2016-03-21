package akashic.storage.service

trait Measure[T] extends Runnable[T] {
  def name: String
  abstract override def run: T = {
    rawLogger.info(s"-> ${name}")
    val start = System.currentTimeMillis
    val result = super.run
    val end = System.currentTimeMillis
    rawLogger.info(s"<- ${name} ${end-start}[ms]")
    result
  }
}
