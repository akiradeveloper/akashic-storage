package akasha.api

trait Version {
  implicit class Version(unwrap: Version) {
    val metaT: Meta = {
      val path: Path = meta.path(meta.get.get)
      val bytes = Paths.toBytes(path)
      Meta.fromBytes(bytes)
    }
  }
}
