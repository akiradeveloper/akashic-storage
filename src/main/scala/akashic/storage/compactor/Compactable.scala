package akashic.storage.compactor

import akashic.storage.server
import java.nio.file.Path

trait Compactable {
  def compact: Iterable[Compactable]
  def dispose(root: Path) {
    server.garbageCan.add(root)
  }
}
