package akashic.storage.patch

import java.nio.file.{Files, Path}

import akashic.storage.backend.{NodePath, Node}

case class Tree(root: NodePath) {
  def bucketPath(name: String): NodePath = root.resolve(name)
  def findBucket(name: String): Option[Bucket] = {
    val path = bucketPath(name)
    if (path.exists) {
      Some(Bucket(path))
    } else {
      None
    }
  }
  def listBuckets: Iterable[Bucket] =
    root.listDir.map(Bucket(_))
}
