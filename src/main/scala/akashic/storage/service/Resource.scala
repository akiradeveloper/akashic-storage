package akashic.storage.service

import java.net.URLDecoder

object Resource {
  val forRoot = "/"
  def forBucket(bucketName: String) = forRoot + bucketName + "/"
  def forObject(bucketName: String, keyName: String) = forBucket(bucketName) + URLDecoder.decode(keyName, "UTF-8")
}
