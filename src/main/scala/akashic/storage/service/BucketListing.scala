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
  }
  case class Group[T <: Filterable](xs: Seq[T]) extends Container[T] {
    require(xs.size > 0)
    val get: T = xs.last
    val size: Int = xs.size
  }

  case class t[T <: Filterable](value: Seq[Container[T]]) {
    def filterByPrefix(prefix: Option[String]): this.type = {
      this
    }

    def takesOnlyAfter(lastName: Option[String]): this.type = {
      this
    }

    def dropWhile(fn: Container[T] => Boolean): this.type = {
      this
    }

    def groupByDelimiter(delimiter: Option[String]): this.type = {
      this
    }

    def byMaxLen(n: Int): (this.type, Boolean) = {
      (this, false)
    }
  }
}
