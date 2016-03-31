package akashic.storage.service

object BucketListing {

  trait Filterable {
    def name: String
  }

  sealed trait Container[T <: Filterable] {
    def get: T
  }
  case class Single[T <: Filterable](x: T) extends Container[T] {
    def get: T = x
    def prefixBy(delimiter: String): String = {
      val s = get.name
      s.indexOf(delimiter) match {
        case -1 => s
        case i => s.slice(0, i) + delimiter
      }
    }
  }
  case class Group[T <: Filterable](xs: Seq[Single[T]], prefix: String) extends Container[T] {
    require(xs.size > 0)
    val get: T = xs.last.get
    val size: Int = xs.size
  }

  implicit class Filtering[T <: Filterable](value: Seq[Single[T]]) {
    def filter(fn: Option[Single[T] => Boolean]) = {
      fn match {
        case Some(p) => value.filter(p)
        case None => value
      }
    }

    def dropWhile(fn: Option[Single[T] => Boolean]) = {
      fn match {
        case Some(p) => value.dropWhile(p)
        case None => value
      }
    }
  }

  implicit class Grouping[T <: Filterable](value: Seq[Single[T]]) {
    def groupByDelimiter(delimiter: Option[String]): Seq[Container[T]] = {
      delimiter match {
        case Some(a) =>
          val deli = encodeKeyName(a)
          val newValue: Seq[Container[T]] = value
            .groupBy(_.prefixBy(deli)).toSeq // [prefix -> seq(contents)]
            .sortBy(_._1) // sort by prefix
            .map { case (prefix, members) =>
              if (members.size > 1) {
                Group(members, prefix.slice(0, prefix.size - deli.size))
              } else {
                members.head
              }
            }
          newValue
        case None => value
      }
    }
  }

  implicit class Truncation[T <: Filterable](value: Seq[Container[T]]) {
    case class Result(value: Seq[Container[T]], truncated: Boolean, nextMarker: Option[Container[T]])
    def truncateByMaxLen(len: Int) = {
      val truncated = if (len == 0) {
        false
      } else {
        value.size > len
      }
      // [spec] All of the keys rolled up in a common prefix count
      // as a single return when calculating the number of returns.
      // So truncate the list after grouping into CommonPrefixes
      val newValue: Seq[Container[T]] = value.take(len)

      // [spec] This element is returned only if you have delimiter request parameter specified.
      // If response does not include the NextMaker and it is truncated,
      // you can use the value of the last Key in the response as the marker in the subsequent request
      // to get the next set of object keys.
      val nextMarker = truncated match {
        case true if len > 0 => Some(newValue(len-1))
        case _ => None
      }
      Result(newValue, truncated, nextMarker)
    }
  }
}
