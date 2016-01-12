package akasha.cleaner

import akasha.Server
import java.nio.file.Path

trait Compactable {
  def server: Server
  def compact: Iterable[Compactable]
  def dispose(root: Path) = {
    server.garbageCan.add(root)
  }
}
