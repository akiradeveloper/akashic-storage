package akashic.storage.backend

import java.io.InputStream

import akka.stream.scaladsl.StreamConverters
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.IOUtils
import org.apache.tika.Tika

/**
 * Any backend stores can talk with akashic-storage by implementing Backend Abstraction Layer (BAL).
 * The idea is like FSAL in nfs-ganesha or Virtual File System (VFS) but is more simpler.
 */
trait BAL {
  def using[A <: AutoCloseable, B](resource: A)(f: A => B): B = {
    try {
      f(resource)
    } finally {
      resource.close
    }
  }

  def getRoot: Node
  def isDirectory(n: Node): Boolean
  def moveNode(n: Node, dir: Node, name: String, replaceIfExists: Boolean)
  def removeNode(n: Node)
  def makeDirectory(dir: Node, name: String): Unit
  def lookup(dir: Node, name: String): Option[Node]
  def listDirectory(n: Node): Iterable[(String, Node)]
  def createFile(dir: Node, name: String, data: Stream[Array[Byte]]): Unit
  def getFileInputStream(n: Node): InputStream
  def getFileAttr(n: Node): FileAttr

  private[backend] def isFile(n: Node): Boolean = !isDirectory(n)
  private[backend] def exists(dir: Node, name: String): Boolean = lookup(dir, name).isDefined
  private[backend] def createFile(dir: Node, name: String, data: Array[Byte]): Unit = {
    createFile(dir, name, Seq(data).toStream)
  }
  private[backend] def getBytes(n: Node): Array[Byte] = IOUtils.toByteArray(getFileInputStream(n))
  private[backend] def getSource(n: Node, chunkSize: Int) = StreamConverters.fromInputStream(() => getFileInputStream(n), chunkSize)
  private[backend] def detectContentType(n: Node): String = using(getFileInputStream(n)) { f =>
    val tika = new Tika()
    tika.detect(f)
  }
  private[backend] def computeMD5(n: Node): Array[Byte] = using(getFileInputStream(n)) { f =>
    DigestUtils.md5(f)
  }
  private[backend] def cleanDirectory(n: Node): Unit = {
    def cleanDirRec(m: Node): Unit = {
      if (isFile(m) || listDirectory(m).isEmpty) {
        removeNode(m)
        return
      }

      listDirectory(m).foreach { case (name: String, l: Node) =>
        cleanDirRec(l)
      }
      removeNode(m)
    }
    listDirectory(n).foreach { case (name: String, m: Node) =>
      cleanDirRec(m)
    }
  }
  private[backend] def purgeDirectory(n: Node) = {
    cleanDirectory(n)
    removeNode(n)
  }
}


