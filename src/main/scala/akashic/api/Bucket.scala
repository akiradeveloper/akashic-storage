package akashic.api

case class Bucket(parent: Tree, root: Path) {
  val acl = LogReader(root.resolve("acl"))
  val cors = LogReader(root.resolve("cors"))
  val versioning = LogReader(root.resolve("versioning"))
  val keys: Path = root.resolve("keys")

  def findKey(name: String): Option[Key] = {
    val path = keys.resolve(name)
    if (Files.exists(keys.resolve(name)) {
      Key(this, path)
    } else { None }
  }
}
