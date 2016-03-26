package akashic.storage.service

import java.net.URLEncoder

import akashic.storage.backend.NodePath
import akka.http.scaladsl.model.{StatusCodes, HttpRequest}
import akka.http.scaladsl.server.Directives._
import java.nio.file.Path

import akashic.storage.server
import akashic.storage.patch.{Commit, Patch, Version, Data}
import com.google.common.hash.Hashing
import com.google.common.io.BaseEncoding
import org.apache.commons.codec.binary.Hex
import org.apache.commons.io.FileUtils
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.xml.{XML, NodeSeq}
import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._

object CompleteMultipartUpload {
  val matcher =
    post &
    extractObject &
    parameter("uploadId") &
    entity(as[String]) &
    extractRequest

  val route = matcher.as(t)(_.run)

  case class t(bucketName: String, keyName: String,
               uploadId: String,
               data: String,
               req: HttpRequest) extends AuthorizedAPI {
    def name = "Complete Multipart Upload"
    def resource = Resource.forObject(bucketName, keyName)
    def runOnce = {
      case class Part(partNumber: Int, eTag: String)
      val bucket = findBucket(server.tree, bucketName)
      val key = findKey(bucket, keyName, Error.NoSuchUpload())
      val upload = findUpload(key, uploadId)

      val parts = Try {
        val xml = XML.loadString(data)
        (xml \ "Part").map { a =>
          Part(
            (a \ "PartNumber").text.toInt,
            (a \ "ETag").text.replace("\"", "")
          )
        }
      } match {
        case Success(a) => a
        case Failure(_) => failWith(Error.MalformedXML())
      }

      if (parts.size == 0)
        failWith(Error.MalformedXML())

      // parts must be sorted
      if (parts != parts.sortBy(_.partNumber))
        failWith(Error.InvalidPartOrder())

      val lastPartNumber = parts.last.partNumber

      for (Part(partNumber, eTag) <- parts) {
        val uploadedPart: NodePath = upload.findPart(partNumber).map(_.unwrap.filePath) match {
          case Some(a) => a
          case None => failWith(Error.InvalidPart())
        }
        val computedETag = Hex.encodeHexString(uploadedPart.computeMD5)
        if (computedETag != eTag) {
          failWith(Error.InvalidPart())
        }
        // Each part must be at least 5MB in size, except the last part.
        if (partNumber < lastPartNumber && uploadedPart.getAttr.length < (5 << 20)) {
          failWith(Error.EntityTooSmall())
        }
      }

      // http://stackoverflow.com/questions/12186993/what-is-the-algorithm-to-compute-the-amazon-s3-etag-for-a-file-larger-than-5gb
      def calcETag(md5s: Seq[String]): String = {
        val bui = new StringBuilder
        for (md5 <- md5s) {
          bui.append(md5)
        }
        val hex = bui.toString
        val raw = BaseEncoding.base16.decode(hex.toUpperCase)
        val hasher = Hashing.md5.newHasher
        hasher.putBytes(raw)
        val digest = hasher.hash.toString
        digest + "-" + md5s.size
      }
      val newETag = calcETag(parts.map(_.eTag))

      val mergeResult: Future[NodeSeq] = Future {
        // the directory is already made
        Commit.replaceDirectory(key.versions.acquireWriteDest) { patch =>
          val versionPatch = Version(key, patch.root)

          val os = versionPatch.data.root.getOutputStream
          for (part <- parts) {
            // parts are all valid so we don't need to call findPart
            os.write(upload.part(part.partNumber).unwrap.get)
          }
          os.close

          versionPatch.acl.put(upload.acl.get)

          val oldMeta = upload.meta.get
          val newMeta = oldMeta.copy(eTag = newETag)
          versionPatch.meta.put(newMeta)
        }

        server.astral.free(upload.root)

        <CompleteMultipartUploadResult>
          <Location>{s"http://${server.address}/${bucketName}/${keyName}"}</Location>
          <Bucket>{bucketName}</Bucket>
          <Key>{keyName}</Key>
          <ETag>{quoteString(newETag)}</ETag>
        </CompleteMultipartUploadResult>
      } recoverWith {
        case e: Throwable =>
          val xml = Error.mkXML(
            Error.withMessage(Error.InternalError("merge parts failed")),
            resource,
            requestId)
          Future(xml)
      }

      val headers = ResponseHeaderList.builder
        .withHeader(X_AMZ_REQUEST_ID, requestId)
        .withHeader(X_AMZ_VERSION_ID, "null")
        .build

      complete(StatusCodes.OK, headers, mergeResult)
    }
  }
}
