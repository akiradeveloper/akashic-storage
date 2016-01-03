package akasha.model

import com.twitter.finagle.http.Status

object Error {

  sealed trait t 
  case class CodeAndMessage(code: Status, message: String)
  
  def mkXML(o: CodeAndMessage, resource: String, requestId: String) = {
    <Error>
      <Code>{o.code}</Code>
      <Message>{o.message}</Message>
      <Resource>{resource}</Resource>
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
}
