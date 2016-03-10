package akashic.storage.patch

import java.nio.file.Path

object Part {
  def apply(path: Path) = new Part(Data.Pure(path))
}
case class Part(unwrap: Data[Array[Byte]]) {
  def id = unwrap.name.toInt
}
