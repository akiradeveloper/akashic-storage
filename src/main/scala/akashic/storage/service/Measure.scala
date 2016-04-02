package akashic.storage.service

trait Measure extends Runnable {
  def name: String
  abstract override def run = {
    logger.debug(s"-> ${name}")
    val start = System.currentTimeMillis
    val result = super.run
    val end = System.currentTimeMillis
    logger.debug(s"<- ${name} ${end-start}[ms]")
    result
  }
}
