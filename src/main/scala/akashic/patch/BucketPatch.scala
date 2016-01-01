package akashic.patch

case class BucketPatch(root: Path) extends Patch {
  val acl = PatchLog(root.resolve("acl"))
  val cors = PatchLog(root.resolve("cors"))
  val versioning = PatchLog(root.resolve("versioning"))
  val keys: Path = root.resolve("keys")

  def init {
    acl.init
    cors.init
    versioning.init
    Files.createDirectory(keys)
  }
}
