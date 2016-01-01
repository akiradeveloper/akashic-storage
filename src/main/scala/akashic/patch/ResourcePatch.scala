package akashic.patch

case class ResourcePatch(root: Path) extends Patch {
  def write(data: Array[Byte]) = ???
  def read: Array[Byte]  = ???
}
