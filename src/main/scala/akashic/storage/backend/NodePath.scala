package akashic.storage.backend

case class NodePath(dir: Node, name: String, var resolved: Option[Node])(implicit fs: FileSystemLike) {
  def self = lookup.get
  def resolve(name: String): NodePath = {
    NodePath(self, name, None)
  }
  def lookup: Option[Node] = {
    if (resolved == None) {
      resolved = fs.lookup(dir, name)
    }
    resolved
  }
  def exists: Boolean = {
    lookup.isDefined
  }
  def listDir: Iterable[NodePath] = {
    fs.listDirectory(self).map(a => NodePath(self, a._1, Some(a._2)))
  }
  def remove = fs.removeNode(self)
  def removeIfExists: Unit = {
    lookup match {
      case Some(n) => fs.removeNode(n)
      case None =>
    }
  }
  def makeDir: Unit = {
    fs.makeDirectory(dir, name)
  }
  def cleanDir = {
    fs.cleanDirectory(self)
  }
  def purgeDir = {
    fs.purgeDirectory(self)
  }
  def getSource(chunkSize: Int) = fs.getSource(self, chunkSize)
  def createFile(data: Stream[Option[Array[Byte]]]) = fs.createFile(dir, name, data)
  def createFile(data: Array[Byte]) = fs.createFile(dir, name, data)
  def readFile: Array[Byte] = fs.getBytes(self)
  def getAttr: FileAttr = fs.getFileAttr(self)
  def moveTo(dir: Node, name: String, replaceIfExists: Boolean) = fs.moveNode(self, dir, name, replaceIfExists)
  def computeMD5 = fs.computeMD5(self)
  def detectContentType: String = fs.detectContentType(self)
}