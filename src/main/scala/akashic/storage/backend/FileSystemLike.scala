package akashic.storage.backend

import java.io.{OutputStream, InputStream}

import akka.stream.scaladsl.StreamConverters
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.IOUtils
import org.apache.tika.Tika

/**
 * Any backend stores can talk with akashic-storage by implementing FileSystemLike.
 * The idea is like FSAL in nfs-ganesha or Virtual File System (VFS) but is more simpler.
 */
trait FileSystemLike {
  def using[A <: AutoCloseable, B](resource: A)(f: A => B): B = {
    try {
      f(resource)
    } finally {
      resource.close
    }
  }

  /**
   * Get the root of the backend
   * We can then traverse the tree structure by calling lookups
   * @return
   */
  def getRoot: Node

  /**
   *
   * @param n
   * @return true if it's a file otherwise false
   */
  def isFile(n: Node): Boolean

  /**
   *
   * @param n
   * @param dir
   * @param name
   * @param replaceIfExists
   */
  def moveNode(n: Node, dir: Node, name: String, replaceIfExists: Boolean)

  /**
   *
   * @param n
   */
  def removeNode(n: Node)

  /**
   *
   * @param dir
   * @param name
   * @return
   */
  def makeDirectory(dir: Node, name: String): Unit

  /**
   * Lookup the child node (but maybe)
   * @param dir parent node
   * @param name relative name of the child
   * @return
   */
  def lookup(dir: Node, name: String): Option[Node]
 /**
   *
   * @param n
   * @return
   */
  def listDirectory(n: Node): Iterable[(String, Node)]

  /**
   *
   * @param dir
   * @param name
   * @return
   */
  def getFileOutputStream(dir: Node, name: String): OutputStream

  /**
   *
   * @param n
   * @return
   */
  def getFileInputStream(n: Node): InputStream

  /**
   *
   * @param n
   * @return
   */
  def getFileAttr(n: Node): FileAttr

  private[backend] def exists(dir: Node, name: String): Boolean = lookup(dir, name).isDefined
  private[backend] def createFile(dir: Node, name: String, data: Array[Byte]): Unit = {
    using(getFileOutputStream(dir, name)) { f =>
      f.write(data)
    }
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


