package akasha

import akasha.patch.Version

package object service {
  implicit class _Version(unwrap: Version) {
    val metaT: Meta.t = {
      val bytes = unwrap.meta.asData.readBytes
      Meta.fromBytes(bytes)
    }
  }
}
