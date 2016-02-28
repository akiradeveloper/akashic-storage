package akashic.storage.service

import akashic.storage.patch.{Upload, Key, Bucket, Tree}

object Error {

  sealed trait t
  case class AccessDenied() extends t
  case class AccountProblem() extends t
  case class AmbiguousGrantByEmailAddress() extends t
  case class BadDigest() extends t
  case class BucketAlreadyExists() extends t
  case class BucketAlreadyOwnByYou() extends t
  case class BucketNotEmpty() extends t
  case class CredentialsNotSupported() extends t
  case class EntityTooLarge() extends t
  case class EntityTooSmall() extends t
  case class ExpireToken() extends t
  case class IllegalVersioningConfigurationException() extends t
  case class IncompleteBody() extends t
  case class IncorrectNumberOfFilesInPostRequest() extends t
  case class InlineDataTooLarge() extends t
  case class InternalError(s: String) extends t
  case class InvalidArgument() extends t
  case class InvalidPart() extends t
  case class InvalidPartOrder() extends t
  case class InvalidToken() extends t
  case class MalformedXML() extends t
  case class NoSuchBucket() extends t
  case class NoSuchKey() extends t
  case class NoSuchUpload() extends t
  case class NoSuchVersion() extends t
  case class NotImplemented() extends t
  case class NotSignedUp() extends t
  case class SignatureDoesNotMatch() extends t
  case class UserKeyMustBeSpecified() extends t

  case class WithMessage(httpCode: Int, errorCode: String, message: String)
  def withMessage(e: t): WithMessage = {
    val tup = e match {
      case AccessDenied() => (403, "Access Denied")
      case BadDigest() => (400, "The Content-MD5 you specified did not match what we received.")
      case BucketAlreadyExists() => (409, "The requested bucket name is not available. The bucket namespace is shared by all users of the system. Please select a different name and try again.")
      case BucketAlreadyOwnByYou() => (409, "Your previous request to create the named bucket succeeded and you already own it.")
      case BucketNotEmpty() => (409, "The bucket you tried to delete is not empty.")
      case EntityTooLarge() => (400, "Your proposed upload exceeds the maximum allowed object size.")
      case EntityTooSmall() => (400, "Your proposed upload is too smaller than the minimum allowed object size. Each part ...")
      case ExpireToken() => (400, "The provided token has expired.")
      case InternalError(s) => (500, s"We encountered an internal error. Please try again. (${s})")
      case InvalidArgument() => (400, "Invalid Argument")
      case InvalidPart() => (400, "One or more of the specified parts could not be found. The part might not have been uploaded, or the specified entity tag might not have matched the part's entity tag.")
      case InvalidPartOrder() => (400, "The list of parts was not in ascending order.Parts list must specified in order by part number.")
      case InvalidToken() => (400, "The provided token is malformed or otherwise invalid.")
      case MalformedXML() => (400, "The XML you provided was not well-formed or did not validate against our published schema.")
      case NoSuchBucket() => (404, "The specified bucket does not exist.")
      case NoSuchKey() => (404, "The specified key does not exist.")
      case NoSuchUpload() => (404, "The specified multipart upload does not exist. The upload ID might be invalid, or the multipart upload might have been aborted or completed.")
      case NoSuchVersion() => (404, "Indicates that the version ID specified in the request does not match an existing version.")
      case NotSignedUp() => (403, "Your account is not signed up")
      case NotImplemented() => (501, "A header you provided implies functionality that is not implemented.")
      case SignatureDoesNotMatch() => (403, "The request signature we calculated does not match the signature you provided. ...")
      case UserKeyMustBeSpecified() => (400, "The bucket POST must contain the specified field name. If it is specified, check the order of the fields.")
      case _ => (500, "unknown error")
    }
    WithMessage(
      httpCode = tup._1,
      errorCode = e.getClass.getSimpleName,
      message = tup._2)
  }

  case class Exception(context: Reportable, e: t) extends RuntimeException

  trait Reportable {
    def requestId: String
    def resource: String
    def failWith(e: t) = throw Error.Exception(this, e)
    def findBucket(tree: Tree, bucketName: String): Bucket = {
      tree.findBucket(bucketName) match {
        case Some(a) => a
        case None => failWith(Error.NoSuchBucket())
      }
    }
    def findKey(bucket: Bucket, keyName: String, e: Error.t = Error.NoSuchKey()): Key = {
      bucket.findKey(keyName) match {
        case Some(a) => a
        case None => failWith(e)
      }
    }
    def findUpload(key: Key, uploadId: String): Upload = {
      key.uploads.findUpload(uploadId) match {
        case Some(a) => a
        case None => failWith(Error.NoSuchUpload())
      }
    }
  }

  def mkXML(o: WithMessage, resource: String, requestId: String) = {
    <Error>
      <Code>{o.errorCode}</Code>
      <Message>{o.message}</Message>
      <Resource>{resource}</Resource>
      <RequestId>{requestId}</RequestId>
    </Error>
  }
}
