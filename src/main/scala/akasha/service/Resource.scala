package akasha.service

object Resource {
  val forRoot = "/"
  def forBucket(bucketName: String) = forRoot + bucketName
  def forObject(bucketName: String, keyName: String) = forBucket(bucketName) + "/" + keyName
}
