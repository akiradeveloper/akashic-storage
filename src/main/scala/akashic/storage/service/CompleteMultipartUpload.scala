package akashic.storage.service

import java.net.URLEncoder
import java.nio.file.Path

import akashic.storage.{files, server}
import akashic.storage.patch.{Commit, Patch, Version, Data}
import akashic.storage.service.Error.Reportable
import com.google.common.hash.Hashing
import com.google.common.io.BaseEncoding
import com.twitter.finagle.http.Request
import com.twitter.util.Future
import org.apache.commons.io.FileUtils

import scala.util.{Failure, Success, Try}
import scala.xml.{XML, NodeSeq}
import io.finch._

object CompleteMultipartUpload {
  val matcher = post(keyMatcher / paramExists("uploadId") ?
    param("uploadId") ?
    body ?
    extractRequest
  ).as[t]
  val endpoint = matcher { a: t => a.run }
  case class t(bucketName: String, keyName: String,
               uploadId: String,
               data: String,
               req: Request) extends Task[Output[Future[NodeSeq]]] {
    def name = "Complete Multipart Upload"
    def resource = Resource.forObject(bucketName, keyName)
    def runOnce = {
      case class Part(partNumber: Int, eTag: String)
      val bucket = findBucket(server.tree, bucketName)
      val key = findKey(bucket, keyName)
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

      // parts must be sorted
      if (parts != parts.sortBy(_.partNumber))
        failWith(Error.InvalidPartOrder())

      val lastPartNumber = parts.last.partNumber

      for (Part(partNumber, eTag) <- parts) {
        val uploadedPart: Path = upload.findPart(partNumber).map(_.unwrap.filePath) match {
          case Some(a) => a
          case None => failWith(Error.InvalidPart())
        }
        if (files.computeMD5(uploadedPart) != eTag) {
          failWith(Error.InvalidPart())
        }
        // Each part must be at least 5MB in size, expect the last part.
        if (partNumber < lastPartNumber && files.fileSize(uploadedPart) < (5 << 20)) {
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

      val mergeFut: Future[NodeSeq] = Future {
        // the directory is already made
        Commit.replaceDirectory(key.versions.acquireWriteDest) { patch =>
          val versionPatch = patch.asVersion

          val aclBytes: Array[Byte] = upload.acl.read
          Commit.replaceData(versionPatch.acl) { patch =>
            val dataPatch = patch.asData
            dataPatch.write(aclBytes)
          }

          val oldMeta = Meta.fromBytes(upload.meta.read)
          val newMeta = oldMeta.copy(eTag = newETag)
          versionPatch.meta.write(newMeta.toBytes)

          files.Implicits.using(FileUtils.openOutputStream(versionPatch.data.filePath.toFile)) { f =>
            for (part <- parts) {
              // parts are all valid so we don't need to call findPart
              f.write(upload.part(part.partNumber).unwrap.read)
            }
          }
        }

        server.astral.free(upload.root)

        <CompleteMultipartUploadResult>
          <Location>{s"http://${server.address}/${bucketName}/${keyName}"}</Location>
          <Bucket>{bucketName}</Bucket>
          <Key>{keyName}</Key>
          <ETag>{quoteString(newETag)}</ETag>
        </CompleteMultipartUploadResult>
      } handle {
        case e: Throwable =>
          Error.mkXML(
            Error.withMessage(Error.InternalError("merge parts failed")),
            resource,
            requestId)
      }

      // TODO versionId (but what if on failure?)
      Ok(mergeFut)
        .withHeader(X_AMZ_REQUEST_ID -> requestId)
        .withHeader(X_AMZ_VERSION_ID -> "null")
    }
  }
}
