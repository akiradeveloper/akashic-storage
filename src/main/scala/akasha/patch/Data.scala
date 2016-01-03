package akasha.patch

case class Data(root: Path) extends Patch with Leaf {
  val data: Path = root.resolve("data")
  def write(inp: InputStream) = ???
  def read: File = ???
  def writeBytes(data: Array[Byte]) = ???
  def readBytes: Array[Byte]  = ???
  def merge(files: Seq[Data]) = ???
  def init {}
}
