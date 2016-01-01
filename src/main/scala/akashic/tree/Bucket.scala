package akashic.tree

import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.util.Try

case class Bucket(path: Path) {
  val acl: PatchLog = path.resolve("acl")
  val cors: PatchLog = path.resolve("cors")
  val versioning: PatchLog = path.resolve("versioning")
  val keys = path.resolve("keys")
}

