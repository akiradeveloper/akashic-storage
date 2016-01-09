package akasha.patch

import java.nio.file.{Files, Path}

import akasha.strings
import akasha.patch.Commit.RetryGeneric

/*
 * uploads/
 *   uploadId/
 *     parts/
 *       partNumber
 */
case class Uploads(root: Path) {
  def init {
    Files.createDirectory(root)
  }
  def acquireNewUpload(fn: Patch => Unit): Upload = RetryGeneric(
    () => {
      val uploadId = strings.random(16)
      root.resolve(uploadId)
    })(fn).run.asUpload
}
