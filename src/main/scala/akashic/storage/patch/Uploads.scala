package akashic.storage.patch

import java.nio.file.{Files, Path}

import akashic.storage.strings
import akashic.storage.patch.Commit.RetryGeneric

/*
 * uploads/
 *   uploadId/
 *     parts/
 *       partNumber
 */
case class Uploads(root: Path) {
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