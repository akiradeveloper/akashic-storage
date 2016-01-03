package akasha.patch

case class Tree(root: Path) {
  def bucket(name: String): Path = root.resolve(name)
  def findBucket(name: String): Option[Bucket] = {
    val path = bucket(name)
    if (Files.exists(path)) {
      Some(Bucket(path))
    } else { None }
  }
  def listBuckets: Seq[Bucket] = ???
}
