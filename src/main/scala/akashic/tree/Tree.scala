package akashic.tree

import java.nio.file._

import scala.util.Try

/*
 * Structure:
 *
 * /bucketName
 *   acl
 *   /keys
 *     /keyName
 *       /versions
 *         /1 (for ordinary put operation)
 *           acl  -- The ACL of an object is set at the object version level.
 *                   By default, PUT sets the ACL of the current version of an object
 *           meta
 *           data
 *         /LS87QP3DUTY0 (for multipart upload)
 *           acl
 *           meta
 *           data (as a result of merging)
 *           parts/
 *             0/ (after initiate multipart upload)
 *               data
 *             1
 *             ...
 */

case class Tree(path: Path) {
  def purge = path.emptyDirectory

  def bucket(name: String): Bucket = {
    new Bucket(path.resolve(name))
  }

  def findBucket(name: String): Try[Bucket] = {
    Try {
      val r = bucket(name)
      r.path.exists.orFailWith(Error.NoSuchBucket())
      r
    }
  }

  def listBuckets: Seq[Bucket] = {
    path.children.map(new Bucket(_))
  }
}
