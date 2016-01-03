package akasha

object Strings {
  def random(n: Int): String = Random.alphanumeric.take(n)
}
