package akashic.storage.compactor

import akashic.storage.Server
import akashic.storage.patch.Upload

case class UploadCompactor(unwrap: Upload, server: Server) extends Compactable {
  override def compact = {
    // compact old parts
    Seq()
  }
}
