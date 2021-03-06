package akashic.storage.backend.impl

import java.io.InputStream
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{Files, Path, Paths, StandardCopyOption}

import akashic.storage.backend._
import com.typesafe.config.Config
import org.apache.commons.io.IOUtils

/** This is the default backend.
 *  With this backend, all data are stored in the local filesystem or network filesystem on a mount point.
 *  THe mountpoinnt should be specified.
 *
 *  backend {
 *    type = akashic.storage.backend.impl.Local
 *    # The mountpoint of the backing filesystem
 *    # User need to prepare this mountpoint before starting akashic-storage server.
 *    mountpoint = /mnt/akashic-storage
 *  }
 */

object Local {
  def fromConfig(config: Config): Local = {
    val mp = Paths.get(config.getString("mountpoint"))
    new Local(mp)
  }
}

class Local(mountpoint: Path) extends BAL {
  implicit def convertImplicitly(n: Node): Path = n.asInstanceOf[Path]
  override def getRoot: Node = {
    require(Files.exists(mountpoint))
    mountpoint
  }
  override def isDirectory(n: Node): Boolean = Files.isDirectory(n)
  override def moveNode(fromDir: Node, fromName: String, toDir: Node, toName: String): Unit = {
    val from = fromDir.resolve(fromName)
    val to = toDir.resolve(toName)
    Files.move(from, to)
  }
  override def removeNode(dir: Node, name: String): Unit = {
    Files.delete(dir.resolve(name))
  }
  override def makeDirectory(dir: Node, name: String): Unit = {
    Files.createDirectory(dir.resolve(name))
  }
  override def listDirectory(n: Node): Iterable[(String, Node)] = {
    import scala.collection.JavaConversions._
    val paths = using(Files.newDirectoryStream(n)) { p =>
      p.iterator.toList
    }
    val names = paths.map(_.getFileName.toString)
    names.zip(paths)
  }
  override def getFileInputStream(n: Node): InputStream = {
    Files.newInputStream(n)
  }
  override def createFile(dir: Node, name: String, data: InputStream): Unit = {
    using(Files.newOutputStream(dir.resolve(name))) { outp =>
      IOUtils.copyLarge(data, outp)
    }
  }
  override def getFileAttr(n: Node): FileAttr = {
    val attr = Files.readAttributes(n, classOf[BasicFileAttributes])
    val creationTime = attr.creationTime().toMillis
    val length = attr.size
    FileAttr(creationTime, length)
  }
  override def lookup(dir: Node, name: String): Option[Node] = {
    val p = dir.resolve(name)
    if (Files.exists(p)) {
      Some(p)
    } else {
      None
    }
  }
}
