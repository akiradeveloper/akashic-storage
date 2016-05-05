package akashic.storage.backend.impl

import java.io.{ByteArrayInputStream, InputStream}

import akashic.storage.backend._
import com.typesafe.config.Config
import org.apache.commons.io.IOUtils

import scala.collection.mutable

object Memory {
  trait Entry
  case class Directory() extends Entry {
    val children = mutable.Map[String, Entry]()
  }
  case class File(data: Array[Byte], attr: FileAttr) extends Entry

  def fromConfig(config: Config): Memory = {
    new Memory()
  }
}

class Memory extends BAL {
  import Memory._
  implicit def convertImplicitly(n: Node): Entry = n.asInstanceOf[Entry]
  val root: Directory = Directory()
  override def getRoot: Node = root
  override def lookup(dir: Node, name: String): Option[Node] = dir.asInstanceOf[Directory].children.get(name)
  override def createFile(dir: Node, name: String, data: InputStream): Unit = {
    val buf = IOUtils.toByteArray(data)
    val attr = FileAttr(System.currentTimeMillis, buf.length)
    val parent = dir.asInstanceOf[Directory]
    parent.children += name -> File(buf, attr)
  }
  override def makeDirectory(dir: Node, name: String): Unit = {
    val parent = dir.asInstanceOf[Directory]
    val newSelf = Directory()
    parent.children += name -> newSelf
  }
  override def listDirectory(n: Node): Iterable[(String, Node)] = {
    val parent = n.asInstanceOf[Directory]
    parent.children
  }
  override def removeNode(dir: Node, name: String): Unit = {
    dir.asInstanceOf[Directory].children -= name
  }
  override def moveNode(fromDir: Node, fromName: String, toDir: Node, toName: String, replaceIfExists: Boolean): Unit = {
    val n = lookup(fromDir, fromName)
    fromDir.asInstanceOf[Directory].children -= fromName
    val newParent = toDir.asInstanceOf[Directory]
    newParent.children += toName -> n
  }
  override def isDirectory(n: Node): Boolean = n.isInstanceOf[Directory]
  override def getFileInputStream(n: Node): InputStream = new ByteArrayInputStream(n.asInstanceOf[File].data)
  override def getFileAttr(n: Node): FileAttr = n.asInstanceOf[File].attr
}
