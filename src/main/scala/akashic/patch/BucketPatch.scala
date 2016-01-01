package akashic.patch

case class BucketPatch(root: Path) extends Patch {
  val acl = PatchLog(root.resolve("acl"))
  val cors = PatchLog(root.resolve("cors"))
  val versioning = PatchLog(root.resolve("versioning"))
  val key(name): Path = root.resolve("keys").resolve(name)
}
