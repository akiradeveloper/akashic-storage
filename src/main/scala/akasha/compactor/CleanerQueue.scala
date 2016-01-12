package akasha.cleaner

import java.util.concurrent.{TimeUnit, LinkedBlockingDeque, ThreadPoolExecutor}
import akasha.Server

case class CleanerQueue() {
  private val workQueue = new ThreadPoolExecutor(1, 1, 1, TimeUnit.SECONDS, new LinkedBlockingDeque[Runnable]())
  def queue(a: Compactable): Unit = {
    workQueue.execute(CleanerWork(a))
  }
  private case class CleanerWork(unwrap: Compactable) extends Runnable {
    def run: Unit = {
      val newTasks = unwrap.compact
      for (t <- newTasks)
        queue(t)
    }
  }
}
