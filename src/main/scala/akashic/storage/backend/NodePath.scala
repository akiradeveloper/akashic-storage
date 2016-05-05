package akashic.storage.backend

import java.io.InputStream

case class NodePath(dir: Node, dirPath: String, name: String, private var resolved: Option[Node])(implicit fs: BAL) {
  private def self = resolve.get
  def path = dirPath + "/" + name
  // Function calls with BAL is likely blocking.
  // We should wrap these functions by blocking so fork-join thread scheduler can switch appropriately.
  // Any code outside backend package should not call BAL functions directly.
  private def withBAL[A](fn: BAL => A): A = scala.concurrent.blocking { fn(fs) }
  def isDirectory: Boolean = withBAL(_.isDirectory(self))
  def apply(name: String): NodePath = {
    require(exists)
    NodePath(self, path, name, None)
  }
  def resolve: Option[Node] = {
    if (resolved == None) {
      resolved = withBAL(_.lookup(dir, name))
    }
    resolved
  }
  def exists: Boolean = resolve.isDefined
  def listDir: Iterable[NodePath] = {
    withBAL(_.listDirectory(self).map(a => NodePath(self, path, a._1, Some(a._2))))
  }
  def remove = withBAL(_.removeNode(dir, name))
  def removeIfExists: Unit =
    resolve match {
      case Some(n) => withBAL(_.removeNode(dir, name))
      case None =>
    }
  def makeDirectory: Unit = withBAL(_.makeDirectory(dir, name))
  def cleanDirectory = withBAL(_.cleanDirectory(self))
  def purgeDirectory = {
    cleanDirectory
    remove
  }
  def getSource(chunkSize: Int) = withBAL(_.getSource(self, chunkSize))
  def createFile(data: InputStream) = withBAL(_.createFile(dir, name, data))
  def createFile(data: Array[Byte]) = withBAL(_.createFile(dir, name, data))
  def getInputStream: InputStream = withBAL(_.getFileInputStream(self))
  def readFile: Array[Byte] = withBAL(_.getBytes(self))
  def getAttr: FileAttr = withBAL(_.getFileAttr(self))
  def moveTo(toDir: Node, toName: String, replaceIfExists: Boolean) = withBAL(_.moveNode(dir, name, toDir, toName, replaceIfExists))
  def computeMD5 = withBAL(_.computeMD5(self))
  def detectContentType: String = withBAL(_.detectContentType(self))
}