package akasha

import scala.util.Random

object Strings {
  def random(n: Int): String = Random.alphanumeric.take(n).mkString
}
