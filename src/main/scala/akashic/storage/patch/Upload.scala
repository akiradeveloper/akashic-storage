package akashic.storage.patch


import akashic.storage.backend.NodePath
import akashic.storage.service.{Acl, Meta}

case class Upload(root: NodePath) extends Patch {
  val parts = root("parts")
  private def partPath(n: Int) = parts(n.toString)
  def part(n: Int) = Part(partPath(n))
  val meta = Meta.makeCache(root("meta"))
  val acl = Acl.makeCache(root("acl"))
  def init {
    parts.makeDirectory
  }
  def findPart(partNumber: Int): Option[Part] = {
    val path = partPath(partNumber)
    if (path.exists) {
      Some(Part(path))
    } else {
      None
    }
  }
  def listParts: Seq[Part] = parts.listDir.map(Part(_)).toSeq.sortBy(_.id)
  def creationTime = meta.root.getAttr.creationTime
}
