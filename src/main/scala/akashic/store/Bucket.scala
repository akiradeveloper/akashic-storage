package akashic.store

import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.util.Try

case class Bucket(path: Path) {

  // exists while processing PUT Bucket request
  val INITFLAG = "creating"
  private def incomplete = path.isEmpty || path.resolve(INITFLAG).exists
  def completed: Boolean = !incomplete
  def commit = path.resolve(INITFLAG).delete

  def mk: Unit = {
    path.mkdirp
    path.resolve(INITFLAG).touch
    keys.mkdirp
  }

  val acl = path.resolve("acl")

  val cors = path.resolve("cors")

  val versioning = path.resolve("versioning")

  val keys = path.resolve("keys")

  def key(name: String): Key = Key(this, keys.resolve(name))

  // "lazy val" because this information shouldn't be changed
  // while we are touching the same bucket instance.
  lazy val versioningEnabled: Boolean = {
    Versioning.read(versioning).value == Versioning.ENABLED
  }

  def findKey(name: String): Try[Key] = {
    Try {
      val r = key(name)
      r.path.exists.orFailWith(Error.NoSuchKey())
      r
    }
  }

  def listKeys: List[Key] = {
    val result = mutable.ListBuffer[Path]()
    Files.walkFileTree(path.resolve("keys"), new SimpleFileVisitor[Path] {
      override def preVisitDirectory(x: Path, attrs: BasicFileAttributes) = {
        // determine if the directory is a key directory by the existance of versions directory
        if (x.resolve("versions").exists) {
          result.add(x)
        }
        FileVisitResult.CONTINUE
      }
    })
    result.toList.map(new Key(this, _))
  }
}

