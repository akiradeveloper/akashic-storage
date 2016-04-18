package akashic.storage.backend

import java.io.{ByteArrayInputStream, InputStream}

import akka.stream.scaladsl.StreamConverters
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.IOUtils
import org.apache.tika.Tika

/**
 * Any backend stores can talk with akashic-storage by implementing Backend Abstraction Layer (BAL).
 * The idea is like FSAL in nfs-ganesha or Virtual File System (VFS) but is more simpler.
 */
trait BAL {
  def getRoot: Node
  def isDirectory(n: Node): Boolean
  def moveNode(fromDir: Node, fromName: String, toDir: Node, toName: String, replaceIfExists: Boolean)
  def removeNode(dir: Node, name: String)
  def makeDirectory(dir: Node, name: String): Unit
  def lookup(dir: Node, name: String): Option[Node]
  def listDirectory(n: Node): Iterable[(String, Node)]
  def createFile(dir: Node, name: String, data: InputStream): Unit
  def getFileInputStream(n: Node): InputStream

  /**
   * Return FileAttr for the file.
   * The implementer doesn't need to implement this method for directories.
   * Because there should be a chance for a backend not to have a attributes on the directory.
   * (or it may not have a structure like directory or bucket internally)
   */
  def getFileAttr(n: Node): FileAttr

  // TODO move to NodePath
  private[backend] def isFile(n: Node): Boolean = !isDirectory(n)
  private[backend] def exists(dir: Node, name: String): Boolean = lookup(dir, name).isDefined
  private[backend] def createFile(dir: Node, name: String, data: Array[Byte]): Unit = using(new ByteArrayInputStream(data)) { inp =>
    createFile(dir, name, inp)
  }
  private[backend] def getBytes(n: Node): Array[Byte] = using(getFileInputStream(n)) { inp =>
    IOUtils.toByteArray(inp)
  }
  private[backend] def getSource(n: Node, chunkSize: Int) = StreamConverters.fromInputStream(() => getFileInputStream(n), chunkSize)
  private[backend] def detectContentType(n: Node): String = using(getFileInputStream(n)) { f =>
    val tika = new Tika()
    tika.detect(f)
  }
  private[backend] def computeMD5(n: Node): Array[Byte] = using(getFileInputStream(n)) { f =>
    DigestUtils.md5(f)
  }
  private[backend] def cleanDirectory(n: Node): Unit = {
    def cleanDirRec(dir: Node, name: String, m: Node): Unit = {
      if (isFile(m) || listDirectory(m).isEmpty) {
        removeNode(dir, name)
        return
      }
      listDirectory(m).foreach { case (name: String, l: Node) =>
        cleanDirRec(m, name, l)
      }
      removeNode(dir, name)
    }
    listDirectory(n).foreach { case (name: String, m: Node) =>
      cleanDirRec(n, name, m)
    }
  }
}
