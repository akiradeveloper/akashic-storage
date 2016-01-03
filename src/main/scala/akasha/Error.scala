package akasha

import com.twitter.finagle.http.Status

object Error {

  sealed trait t 
  case class CodeAndMessage(code: Status, message: String)
  
  // FIXME resource should be bucketName/keyName. not uri.path.toString
  def mkXML(o: CodeAndMessage, resource: String, requestId: String) = {
    <Error>
      <Code>{o.code}</Code>
      <Message>{o.message}</Message>
      <Resource>{resource.toString}</Resource>
      <RequestId>{requestId}</RequestId>
    </Error>
  }

  case class Exception(e: t) extends RuntimeException

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
  // TODO

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

  def toCodeAndMessage(e: t): CodeAndMessage = {
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
    CodeAndMessage(tup._1, tup._2)
  }
}
