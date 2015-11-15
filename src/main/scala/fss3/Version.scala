package fss3

import java.nio.file.Path

case class Version(parent: Key, path: Path) {

  // exists while a PUT Object request is in processing
  //
  // Note:
  // creating buckets and objects aren't transaction
  // to know which one failed in partial state
  // we first write a flag before starting transaction
  // and delete it after processing.
  val INITFLAG = "creating"
  def completed = !(path.children.isEmpty || path.resolve(INITFLAG).exists)
  def commit = path.resolve(INITFLAG).delete

  def mk: Unit = {
    path.mkdirp
    path.resolve(INITFLAG).touch
  }

  val data = path.resolve("data")

  val acl = path.resolve("acl")

  val meta = path.resolve("meta")

  def getKeyName = parent.getKeyName

  def id: Int = {
    path.lastName.toString.toInt
  }

  def metaT = Meta.read(meta)
}
