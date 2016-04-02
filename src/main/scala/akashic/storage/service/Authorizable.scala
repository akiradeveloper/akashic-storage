package akashic.storage.service

trait Authorizable extends Runnable {
  def requestId: String
  def resource: String
  var callerId: String = ""
  abstract override def run = {
    authorizeS3v2(resource, requestId) { ci =>
      callerId = ci
      super.run
    }
  }
}
