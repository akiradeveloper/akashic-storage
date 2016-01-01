package akashic.patch

case class DataPatch(root: Path) extends Patch {
  val data: Path = root.resolve("data")
  def write(inp: InputStream) = ???
  def read: File = ???
  def init {}
}
