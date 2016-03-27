package akashic.storage.backend

import java.io.InputStream

object Streams {
  val eod = Stream[Option[Array[Byte]]](None)
  def inputStreamFromStream(data: Stream[Option[Array[Byte]]]): InputStream = {
    val validData = data.takeWhile(_.isDefined)
    new InputStream {
      var eod = false
      var currentOpt #:: rest = validData
      var current = currentOpt.get
      var pos: Int = 0
      override def read(): Int = {
        if (eod)
          return -1
        val b = current(pos)
        pos += 1
        if (pos > current.size - 1) {
          if (rest == Stream.empty) {
            eod = true
            return b
          }
          val currentOpt #:: tmpRest = rest
          rest = tmpRest
          current = currentOpt.get
          pos = 0
        }
        b
      }
    }
  }
}
