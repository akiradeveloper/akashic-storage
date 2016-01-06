package akasha.model

object Error {

  sealed trait t 
  case class WithMessage(httpCode: Int, errorCode: String, message: String)

  def withMessage(e: t): WithMessage = {
    val tup = e match {
      case BadDigest() => (400, "The Content-MD5 you specified did not match what we received.")
      case NotSignedUp() => (403, "Your account is not signed up for the albero S3 service.")
      case AccessDenied() => (403, "Access Denied")
      case InvalidToken() => (400, "The provided token is malformed or otherwise invalid.")
      case ExpireToken() => (400, "The provided token has expired.")
      case SignatureDoesNotMatch() => (403, "The request signature we calculated does not match the signature you provided. ...")
      case NoSuchBucket() => (404, "The specified bucket does not exist.")
      case NoSuchKey() => (404, "The specified key does not exist.")
      case InternalError(s) => (500, s"We encountered an internal error. Please try again. (${s})")
      case BucketAlreadyExists() => (409, "The requested bucket name is not available. The bucket namespace is shared by all users of the system. Please select a different name and try again.")
      case MalformedXML() => (400, "The XML you provided was not well-formed or did not validate against our published schema.")
      case NotImplemented() => (501, "A header you provided implies functionality that is not implemented.")

      case EntityTooSmall() => (400, "Your proposed upload is too smaller than the minimum allowed object size. Each part ...")
      case InvalidPart() => (400, "One or more of the specified parts could not be found. The part might not have been uploaded, or the specified entity tag might not have matched the part's entity tag.")
      case InvalidPartOrder() => (400, "The list of parts was not in ascending order.Parts list must specified in order by part number.")
      case NoSuchUpload() => (404, "The specified multipart upload does not exist. The upload ID might be invalid, or the multipart upload might have been aborted or completed.")
      case _ => (500, "unknown error")
    }
    WithMessage(
      httpCode = tup._1,
      errorCode = e.getClass.getSimpleName,
      message = tup._2)
  }

  def mkXML(o: WithMessage, resource: String, requestId: String) = {
    <Error>
      <Code>{o.errorCode}</Code>
      <Message>{o.message}</Message>
      <Resource>{resource}</Resource>
      <RequestId>{requestId}</RequestId>
    </Error>
  }

  case class Exception(context: Context, e: t) extends RuntimeException

  case class AccessDenied() extends t
  case class AccountProblem() extends t
  case class AmbiguousGrantByEmailAddress() extends t
  case class BadDigest() extends t
  case class BucketAlreadyOwnByYou() extends t
  case class BucketNotEmpty() extends t
  case class CredentialsNotSupported() extends t
  case class EntityTooLarge() extends t
  case class IllegalVersioningConfigurationException() extends t
  case class IncompleteBody() extends t
  case class IncorrectNumberOfFilesInPostRequest() extends t
  case class InlineDataTooLarge() extends t

  case class NotSignedUp() extends t
  case class InvalidToken() extends t
  case class ExpireToken() extends t
  case class SignatureDoesNotMatch() extends t
  case class NoSuchBucket() extends t
  case class NoSuchKey() extends t
  case class InternalError(s: String) extends t
  case class MalformedXML() extends t
  case class BucketAlreadyExists() extends t
  case class NotImplemented() extends t

  // Complte Multipart Upload specific
  case class EntityTooSmall() extends t
  case class InvalidPart() extends t
  case class InvalidPartOrder() extends t
  case class NoSuchUpload() extends t
}
