package akashic.tree

import java.nio.file.Path

case class Version(parent: Key, path: Path) {
  val data = path.resolve("data")
  val acl = path.resolve("acl")
  val meta = path.resolve("meta")
  def getKeyName = parent.getKeyName
  def id = path.lastName.toString.toInt
}
