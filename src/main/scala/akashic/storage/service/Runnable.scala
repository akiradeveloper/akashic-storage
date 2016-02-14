package akashic.storage.service

trait Runnable[T] {
  def run: T
}
