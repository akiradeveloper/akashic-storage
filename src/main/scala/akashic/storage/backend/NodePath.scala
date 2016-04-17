package akashic.storage.backend

import java.io.InputStream

case class NodePath(dir: Node, name: String, private var resolved: Option[Node])(implicit fs: BAL) {
  private def self = lookup.get
  // Function calls with BAL is likely blocking.
  // We should wrap these functions by blocking so fork-join thread scheduler can switch appropriately.
  // Any code outside backend package should not call BAL functions directly.
  private def withBAL[A](fn: BAL => A): A = scala.concurrent.blocking { fn(fs) }
  def resolve(name: String): NodePath = NodePath(self, name, None)
  def lookup: Option[Node] = {
    if (resolved == None) {
      resolved = withBAL(_.lookup(dir, name))
    }
    resolved
  }
  def exists: Boolean = lookup.isDefined
  def listDir: Iterable[NodePath] = {
    withBAL(_.listDirectory(self).map(a => NodePath(self, a._1, Some(a._2))))
  }
  def remove = withBAL(_.removeNode(self))
  def removeIfExists: Unit =
    lookup match {
      case Some(n) => withBAL(_.removeNode(n))
      case None =>
    }
  def makeDirectory: Unit = withBAL(_.makeDirectory(dir, name))
  def cleanDirectory = withBAL(_.cleanDirectory(self))
  def purgeDirectory = withBAL(_.purgeDirectory(self))
  def getSource(chunkSize: Int) = withBAL(_.getSource(self, chunkSize))
  def createFile(data: InputStream) = withBAL(_.createFile(dir, name, data))
  def createFile(data: Array[Byte]) = withBAL(_.createFile(dir, name, data))
  def getInputStream: InputStream = withBAL(_.getFileInputStream(self))
  def readFile: Array[Byte] = withBAL(_.getBytes(self))
  def getAttr: FileAttr = withBAL(_.getFileAttr(self))
  def moveTo(dir: Node, name: String, replaceIfExists: Boolean) = withBAL(_.moveNode(self, dir, name, replaceIfExists))
  def computeMD5 = withBAL(_.computeMD5(self))
  def detectContentType: String = withBAL(_.detectContentType(self))
}