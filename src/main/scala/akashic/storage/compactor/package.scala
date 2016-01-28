package akashic.storage

import java.nio.file.Path
import akashic.storage.server

package object compactor {
  def dispose(path: Path) = server.astral.dispose(path)
}
