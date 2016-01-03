package akasha.http

import akasha.model.{Error => E}

object Error {
  def toCodeAndMessage(e: E.t): E.CodeAndMessage = {
    val tup = e match {
      case BadDigest() => (Status.BadRequest, "The Content-MD5 you specified did not match what we received.")
      case NotSignedUp() => (Status.Forbidden, "Your account is not signed up for the albero S3 service.")
      case AccessDenied() => (Status.Forbidden, "Access Denied")
      case InvalidToken() => (Status.BadRequest, "The provided token is malformed or otherwise invalid.")
      case ExpireToken() => (Status.BadRequest, "The provided token has expired.")
      case SignatureDoesNotMatch() => (Status.Forbidden, "The request signature we calculated does not match the signature you provided. ...")
      case NoSuchBucket() => (Status.NotFound, "The specified bucket does not exist.")
      case NoSuchKey() => (Status.NotFound, "The specified key does not exist.")
      case InternalError(s) => (Status.InternalServerError, s"We encountered an internal error. Please try again. (${s})")
      case BucketAlreadyExists() => (Status.Conflict, "The requested bucket name is not available. The bucket namespace is shared by all users of the system. Please select a different name and try again.")
      case MalformedXML() => (Status.BadRequest, "The XML you provided was not well-formed or did not validate against our published schema.")
      case NotImplemented() => (Status.NotImplemented, "A header you provided implies functionality that is not implemented.")

      case EntityTooSmall() => (Status.BadRequest, "Your proposed upload is too smaller than the minimum allowed object size. Each part ...")
      case InvalidPart() => (Status.BadRequest, "")
      case InvalidPartOrder() => (Status.BadRequest, "")
      case NoSuchUpload() => (Status.NotFound, "")
      case _ => (Status.InternalServerError, "unknown error")
    }
    E.CodeAndMessage(tup._1, tup._2)
  }
}
