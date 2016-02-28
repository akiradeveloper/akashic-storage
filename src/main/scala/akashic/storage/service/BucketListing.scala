package akashic.storage.service

object BucketListing {

  trait Filterable {
    def name: String
  }

  trait Container[T <: Filterable] {
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
    def takesOnlyAfter(lastName: Option[String]) = {
      lastName match {
        case Some(a) => value.dropWhile(_.get.name < a)
        case None => value
      }
    }

    def filterByPrefix(prefix: Option[String]) = {
      prefix match {
        case Some(a) => value.filter(_.get.name.startsWith(a))
        case None => value
      }
    }

    def dropWhile(fn: Single[T] => Boolean) = {
      value.dropWhile(fn)
    }
  }

  implicit class Grouping[T <: Filterable](value: Seq[Single[T]]) {
    def groupByDelimiter(delimiter: Option[String]): Seq[Container[T]] = {
      delimiter match {
        case Some(a) =>
          val deli = encodeKeyName(a)
          val newValue: Seq[Container[T]] =
            value
            .groupBy(_.prefixBy(deli)).toSeq // [prefix -> seq(contents)]
            .sortBy(_._1) // sort by prefix
            .map { case (prefix, members) =>
              if (members.size > 1) {
                Group(members, prefix.slice(0, prefix.size - deli.size))
              } else {
                members(0)
              }
            }
          newValue
        case None => value
      }
    }
  }

  implicit class Truncation[T <: Filterable](value: Seq[Container[T]]) {
    def byMaxLen(n: Int) = {
      (value, false)
    }
  }
}
