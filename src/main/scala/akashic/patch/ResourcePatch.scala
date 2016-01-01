package akashic.patch

case class ResourcePatch(root: Path) extends Patch {
  def data: Path = root.resolve("data")
  def write(data: Array[Byte]) = ???
  def read: Array[Byte]  = ???
}
