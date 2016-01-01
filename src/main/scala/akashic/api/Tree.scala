package akashic.tree

case class Tree(root: Path) {
  def bucket(name: String): Path = root.resolve(name)
  def findBucket(name: String): Option[Bucket] = {
    val path = bucket(name)
    if (Files.exists(path)) {
      Some(Bucket(this, path)
    } else { None }
  }
}
