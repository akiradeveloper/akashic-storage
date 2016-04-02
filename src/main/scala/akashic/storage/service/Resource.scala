package akashic.storage.service

object Resource {
  val forRoot = "/"
  def forBucket(bucketName: String) = forRoot + bucketName + "/"
  def forObject(bucketName: String, keyName: String) = forBucket(bucketName) + decodeKeyName(keyName)
}
