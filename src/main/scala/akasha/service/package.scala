package akasha

import akasha.patch.Version

package object service {
  implicit class _Version(unwrap: Version) {
    val metaT: Meta.t = {
      val patch = unwrap.meta.get.get
      val bytes = patch.asData.readBytes
      Meta.fromBytes(bytes)
    }
  }
}
