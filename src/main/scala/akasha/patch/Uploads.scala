package akasha.patch

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
      val uploadId = akasha.Strings.random(16)
      root.resolve(uploadId)
    }, fn).run.asUpload
}