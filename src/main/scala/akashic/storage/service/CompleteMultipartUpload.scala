package akashic.storage.service

import java.io.{ByteArrayInputStream, SequenceInputStream}
import java.util.Collections

import akashic.storage.backend.{NodePath, Streams}
import akashic.storage.patch.{Commit, Version}
import akashic.storage.server
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import com.google.common.hash.Hashing
import com.google.common.io.BaseEncoding
import org.apache.commons.codec.binary.Hex

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.xml.{NodeSeq, XML}

object CompleteMultipartUpload {
  val matcher =
    post &
    extractObject &
    parameter("uploadId") &
    entity(as[String])

  val route = matcher.as(t)(_.run)

  case class t(bucketName: String, keyName: String,
               uploadId: String,
               data: String) extends AuthorizedAPI {
    def name = "Complete Multipart Upload"
    def resource = Resource.forObject(bucketName, keyName)
    def runOnce = {
      case class Part(partNumber: Int, eTag: String)
      val bucket = findBucket(server.tree, bucketName)
      val key = findKey(bucket, keyName, Error.NoSuchUpload())
      val upload = findUpload(key, uploadId)

      val parts: Seq[Part] = Try {
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

          val streams = parts.toStream.map(part => upload.part(part.partNumber).unwrap.filePath.getInputStream)
          using(new SequenceInputStream(Collections.enumeration(streams)))(versionPatch.data.root.createFile)

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
