package akashic.api

case class Key(parent: Bucket, root: Path) {
  val versions = path.resolve("versions")

  def findVersion(id: Int): Option[Version] = ???

  def maxVersionId: Option[Int] = LogReader(versions).get.map(Version(_).id)
}
