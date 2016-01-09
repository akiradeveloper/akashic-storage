package akasha

import scala.util.Random

object strings {
  def random(n: Int): String = Random.alphanumeric.take(n).mkString
}
