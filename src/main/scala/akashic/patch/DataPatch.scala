package akashic.patch

case class DataPatch(root: Path) {
  def write(inp: InputStream) = ???
  def read: File = ???
}
