package akashic.storage.compactor

import java.nio.file.Path

trait Compactable {
  def compact: Iterable[Compactable]
}
