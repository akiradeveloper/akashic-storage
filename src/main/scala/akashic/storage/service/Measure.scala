package akashic.storage.service

trait Measure[T] extends Runnable[T] {
  def name: String
  abstract override def run: T = {
    logger.debug(s"-> ${name}")
    val start = System.currentTimeMillis
    val result = super.run
    val end = System.currentTimeMillis
    logger.debug(s"<- ${name} ${end-start}[ms]")
    result
  }
}
