package akasha

import akasha.patch.Version

package object service {
  implicit class _Version(unwrap: Version) {
    val metaT: Meta.t = {
      val bytes = unwrap.meta.asData.readBytes
      Meta.fromBytes(bytes)
    }
  }
  implicit class _Option[A](unwrap: Option[A]) {
    def `<+`(other: Option[A]): Option[A] = {
      unwrap match {
        case Some(a) => unwrap
        case None => other
      }
    }
  }
}
