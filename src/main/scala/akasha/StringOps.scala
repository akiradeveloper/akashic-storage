package akasha

import scala.util.Random

object StringOps {
  def random(n: Int): String = Random.alphanumeric.take(n).mkString
}
