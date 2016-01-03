package akasha.patch

/*
 * uploads/
 *   uploadId/
 *     parts/
 *       partNumber
 */
case class Uploads(root: Path, fn: Upload => Unit) {
  def acquireNewUpload: Upload = RetryGeneric(
    () => {
      val uploadId = akasha.Strings.random(16)
      root.resolve(uploadId)
    }, fn).run.asUpload
}
