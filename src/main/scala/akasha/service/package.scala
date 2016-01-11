package akasha

import akasha.patch.Version

import io.finch._

package object service {
  // first appearance wins
  implicit class _Option[A](unwrap: Option[A]) {
    def `<+`(other: Option[A]): Option[A] = 
      unwrap match {
        case Some(a) => unwrap
        case None => other
      }
  }

  implicit class _Output[A](unwrap: Output[A]) {
    def append(list: KVList.t): Output[A] = list.unwrap.foldLeft(unwrap) { (acc, a) => acc.withHeader(a) }
  }
}
