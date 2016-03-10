package akashic.storage.admin

import java.nio.file.{Files, Path}

import akashic.storage.caching.{CacheMap, Cache}
import akashic.storage.patch.{Data, Commit}
import akashic.storage.{files, strings}

import scala.pickling.Defaults._
import scala.pickling.binary._

case class UserTable(root: Path) {
  val dbPath = root.resolve("db")
  def makeCache(path: Path) = new Cache[InMem] {
    override def cacheMap: CacheMap[K, InMem] = new CacheMap.Null[K, InMem]()
    private def doWriter(a: InMem): Array[Byte] = {
      a.toByteArray
    }
    private def doReader(a: Array[Byte]): InMem = {
      val list = BinaryPickle(a).unpickle[Seq[User.t]]
      val userMap = list.map(a => (a.id, a)).toMap
      InMem(userMap.values)
    }
    override def writer: (InMem) => Array[Byte] = doWriter
    override def reader = doReader
    override val filePath: Path = path
  }
  val dbData = makeCache(dbPath)
  dbData.put(InMem(Map(), Map()))

  object InMem {
    def apply(ls: Iterable[User.t]): InMem = {
      val newUserMap = ls.map(a => (a.id, a)).toMap
      val newIdMap = newUserMap.map {
        case (id, user) => (user.accessKey, id)
      }
      InMem(newUserMap, newIdMap)
    }
  }
  case class InMem(
    userMap: Map[String, User.t], // id -> User
    idMap: Map[String, String] // accessKey -> id
  ){
    def add(user: User.t): InMem = add(user.id, user)
    def add(id: String, user: User.t): InMem = {
      val newUserMap = userMap + (id -> user)
      InMem(newUserMap.values)
    }
    def remove(id: String): InMem = {
      val newUserMap = userMap - id
      InMem(newUserMap.values)
    }
    def toByteArray = {
      userMap.values.toSeq.pickle.value
    }
    def commit {
      Commit.replaceData(dbData, makeCache) { data =>
        data.put(this)
      }
    }
  }

  def getId(accessKey: String): Option[String] = {
    dbData.get.idMap.get(accessKey)
  }

  def getUser(id: String): Option[User.t] = {
    dbData.get.userMap.get(id)
  }

  def addUser(user: User.t): Unit = {
    dbData.get.add(user).commit
  }

  private def mkRandUser: User.t = {
    User.t(
      id = strings.random(64),
      accessKey = strings.random(20).toUpperCase,
      secretKey = strings.random(40),
      name = "noname",
      email = "noname@noname.org",
      displayName = "noname"
    )
  }

  def mkUser: User.t = {
    val newUser = mkRandUser
    dbData.get.add(newUser).commit
    newUser
  }

  def updateUser(id: String, user: User.t): Unit = {
    dbData.get.remove(id).add(user).commit
  }
}
