package akashic.storage.compactor

import java.util.concurrent.{TimeUnit, LinkedBlockingDeque, ThreadPoolExecutor}
import akashic.storage.Server

case class CompactorQueue() {
  private val workQueue = new ThreadPoolExecutor(1, 1, 1, TimeUnit.SECONDS, new LinkedBlockingDeque[Runnable]())
  def queue(a: Compactable): Unit = {
    workQueue.execute(CompactorWork(a))
  }
  private case class CompactorWork(unwrap: Compactable) extends Runnable {
    def run: Unit = {
      val newTasks = unwrap.compact
      for (t <- newTasks)
        queue(t)
    }
  }
}
