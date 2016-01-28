package akashic.storage.patch

import java.nio.file.{Files, Path}

import akashic.storage.{files, strings}

case class Uploads(root: Path) {
  def acquireNewUpload: String = {
    val uploadId = strings.random(32)
    uploadId
  }
  def findUpload(uploadId: String): Option[Upload] = {
    val uploadPath = root.resolve(uploadId)
    if (Files.exists(uploadPath)) {
      Some(Upload(uploadPath))
    } else {
      None
    }
  }
  def listUploads: Iterable[Upload] = files.children(root).map(Upload(_))
}
