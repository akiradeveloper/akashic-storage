package akashic.storage.patch

import akashic.storage.backend.NodePath
import akashic.storage.strings

case class Uploads(root: NodePath) {
  def acquireNewUpload: String = {
    val uploadId = strings.random(32)
    uploadId
  }
  def findUpload(uploadId: String): Option[Upload] = {
    val uploadPath = root(uploadId)
    if (uploadPath.exists) {
      Some(Upload(uploadPath))
    } else {
      None
    }
  }
  def listUploads: Iterable[Upload] = root.listDir.map(Upload(_))
}
