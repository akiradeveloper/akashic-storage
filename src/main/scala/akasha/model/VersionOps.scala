package akasha.model

import akasha.patch.Version

trait VersionOps {
  implicit class VersionOps(unwrap: Version) {
    val metaT: Meta = {
      val path: Path = meta.path(meta.get.get)
      val bytes = Paths.toBytes(path)
      Meta.fromBytes(bytes)
    }
  }
}
