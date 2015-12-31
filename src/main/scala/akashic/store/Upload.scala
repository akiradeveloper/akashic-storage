package akashic.store

import java.io.BufferedOutputStream
import java.nio.file.{Files, Path, StandardCopyOption}

import org.apache.commons.io.IOUtils

import scala.collection.JavaConversions._

case class Upload(parent: Key, path: Path) {

  // exists while this upload is being initiated
  // (not until completed)
  val INITFLAG = "initiating"
  // FIXME
  // path.isEmpty uses collection's implicit To-Java conversion
  // is this correct?
  private def incomplete = path.isEmpty || path.resolve(INITFLAG).exists
  def initiationCompleted = !incomplete
  def completeInitiation = path.resolve(INITFLAG).delete

  val parts = path.resolve("parts")

  def mk: Unit = {
    path.mkdirp
    path.resolve(INITFLAG).touch
    parts.mkdirp
  }

  val acl = path.resolve("acl")

  val meta = path.resolve("meta")

  // FIXME (H) tmp and rename technique should be removed
  case class Part(path: Path) {
    val INITFLAG = "uploading"
    def mk = path.mkdirp
    def id = path.lastName.toInt
    def data = path.resolve("data")
    def completed = !(path.children.isEmpty || path.resolve(INITFLAG).exists)
    def write(dat: Array[Byte]) {
      path.resolve(INITFLAG).touch
      data.writeBytes(dat)
      path.resolve(INITFLAG).delete
    }
  }

  def partById(partId: Int): Part = Part(parts.resolve(partId.toString))

  def acquirePart(partId: Int): Part = {
    val p = partById(partId)
    p.mk
    p
  }

  def id = path.lastName

  def listParts: Seq[Part] = parts.children.map(Part(_)).sortBy(_.id)

  def mergeParts(parts: Seq[Int]): Unit = {
    using(new BufferedOutputStream(Files.newOutputStream(path.resolve("data")))) { out =>
      for (part <- parts.map(partById(_))) {
        using(Files.newInputStream(part.data)) { in =>
          IOUtils.copy(in, out)
        }
      }
    }
  }

  def completeMerging: Unit = {
    val newVer = parent.acquireNewVersion
    newVer.path.emptyDirectory // We need to purge here because replacing existing path requires the dest doesn't have decendants
    Files.move(path, newVer.path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
  }
}
