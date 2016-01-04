package akasha.model

import java.nio.file.{Paths, Path}

import akasha.patch.Version

trait VersionOps {
  implicit class VersionOps(unwrap: Version) {
    val metaT: Meta.t = {
      val patch = unwrap.meta.get.get
      val bytes = patch.asData.readBytes
      Meta.fromBytes(bytes)
    }
  }
}
