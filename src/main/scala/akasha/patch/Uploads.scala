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
  def acquireNewUpload(id: String): String = {
    // e.g. 1-akiradeveloper (16 digits)
    val uploadId = id + "-" + strings.random(16 - 1 - id.length)
    uploadId
  }
  def findUpload(uploadId: String): Option[Upload] = {
    val uploadPath = root.resolve(uploadId)
    if (Files.exists(uploadPath) && Upload(uploadPath).committed) {
      Some(Upload(uploadPath))
    } else {
      None
    }
  }
}
