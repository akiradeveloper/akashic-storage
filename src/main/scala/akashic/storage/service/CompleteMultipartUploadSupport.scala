package akashic.storage.service

import java.net.URLEncoder
import java.nio.file.Path

import akashic.storage.{files, Server}
import akashic.storage.patch.{Commit, Patch, Version, Data}
import akashic.storage.service.Error.Reportable
import com.google.common.hash.Hashing
import com.google.common.io.BaseEncoding
import com.twitter.util.Future
import org.apache.commons.io.FileUtils

import scala.util.{Failure, Success, Try}
import scala.xml.{XML, NodeSeq}
import io.finch._

trait CompleteMultipartUploadSupport {
  self: Server =>
  object CompleteMultipartUpload {
    val matcher = post(string / string ?
      param("uploadId") ?
      body ?
      RequestId.reader ?
      CallerId.reader
    ).as[t]
    val endpoint = matcher { a: t => a.run }
    case class t(bucketName: String, keyName: String,
                 uploadId: String,
                 data: String,
                 requestId: String,
                 callerId: String) extends Task[Output[Future[NodeSeq]]] with Reportable {
      def name = "Complete Multipart Upload"
      def resource = Resource.forObject(bucketName, keyName)
      def runOnce = {
        case class Part(partNumber: Int, eTag: String)
        val bucket = findBucket(tree, bucketName)
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
          val uploadedPart: Path = upload.findPart(partNumber).flatMap(_.versions.get).map(_.asData.filePath) match {
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
          // uploadId = $versionId-$random (summed up in 16 chars)
          val versionId = uploadId.split("-")(0).toInt

          // the directory is already made
          val versionPatch: Version = key.versions.get(versionId).get.asVersion

          // we need to clean the directory
          // because this may be the second complete request
          // (should purge the old stuffs)
          FileUtils.cleanDirectory(versionPatch.root.toFile)

          versionPatch.init

          val aclBytes: Array[Byte] = upload.acl.readBytes
          Commit.retry(versionPatch.acl) { patch =>
            val dataPatch = patch.asData
            dataPatch.init

            dataPatch.writeBytes(aclBytes)
          }

          val oldMeta = Meta.fromBytes(upload.meta.readBytes)
          val newMeta = oldMeta.copy(eTag = newETag)
          versionPatch.meta.writeBytes(newMeta.toBytes)

          files.Implicits.using(FileUtils.openOutputStream(versionPatch.data.filePath.toFile)) { f =>
            for (part <- parts) {
              f.write(upload.findPart(part.partNumber).get.versions.get.get.asData.readBytes)
            }
          }

          versionPatch.commit

          <CompleteMultipartUploadResult>
            <Location>{s"http://${address}/${bucketName}/${URLEncoder.encode(keyName)}"}</Location>
            <Bucket>{bucketName}</Bucket>
            <Key>{keyName}</Key>
            <ETag>{newETag}</ETag>
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
      }
    }
  }
}
