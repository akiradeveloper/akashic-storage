package akashic.storage.patch

import java.nio.file.Path

object Part {
  def apply(path: Path) = new Part(Data(path))
}
case class Part(unwrap: Data) {
  def id = unwrap.name.toInt
}
