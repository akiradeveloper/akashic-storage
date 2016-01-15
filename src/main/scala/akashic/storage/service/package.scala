package akashic.storage

import akashic.storage.patch.Version

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

  val X_AMZ_REQUEST_ID = "x-amz-request-id"
  val X_AMZ_VERSION_ID = "x-amz-version-id"
  val X_AMZ_DELETE_MARKER = "x-amz-delete-marker"
}
