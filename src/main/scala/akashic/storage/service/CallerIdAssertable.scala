package akashic.storage.service

trait CallerIdAssertable extends Error.Reportable {
  def callerId: String
  def checkCallerId: Unit = {
    if (callerId == "") {
      failWith(Error.SignatureDoesNotMatch())
    }
  }
}
