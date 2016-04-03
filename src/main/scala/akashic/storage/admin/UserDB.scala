package akashic.storage.admin

import akashic.storage.auth.CallerId
import akashic.storage.backend.NodePath
import akashic.storage.caching.{Cache, CacheMap}
import akashic.storage.patch.Commit
import akashic.storage.strings
import com.google.common.cache.CacheBuilder

import scala.pickling.Defaults._
import scala.pickling.binary._

case class UserDB(root: NodePath) {
  val cache = new CacheMap.Guava[String, InMem](CacheBuilder.newBuilder
    .maximumSize(1)
    .build())

  val dbPath = root.resolve("db")
  def makeCache(path: NodePath) = new Cache[InMem] {
    override def cacheMap: CacheMap[K, InMem] = cache
    private def doWriter(a: InMem): Array[Byte] = {
      a.toByteArray
    }
    private def doReader(a: Array[Byte]): InMem = {
      val list = BinaryPickle(a).unpickle[Seq[User]]
      val userMap = list.map(a => (a.id, a)).toMap
      InMem(userMap.values)
    }
    override def writer: (InMem) => Array[Byte] = doWriter
    override def reader = doReader
    override val filePath = path
  }
  val dbData = makeCache(dbPath)
  if (!dbPath.exists)
    dbData.put(InMem(Iterable()))

  object InMem {
    def apply(ls: Iterable[User]): InMem = {
      val newUserMap = ls.map(a => (a.id, a)).toMap
      val newIdMap = newUserMap.map {
        case (id, user) => (user.accessKey, id)
      }
      InMem(newUserMap, newIdMap)
    }
  }
  case class InMem(
    userMap: Map[String, User], // id -> User
    idMap: Map[String, String] // accessKey -> id
  ){
    def add(user: User): InMem = add(user.id, user)
    def add(id: String, user: User): InMem = {
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

  def find(id: String): Option[User] = {
    id match {
      case CallerId.ANONYMOUS => Some(User.Anonymous)
      case id => dbData.get.userMap.get(id)
    }
  }

  def add(user: User): Unit = {
    dbData.get.add(user).commit
  }

  private def mkRandUser: User = {
    val randName = strings.random(20)
    User(
      id = strings.random(64),
      accessKey = strings.random(20).toUpperCase,
      secretKey = strings.random(40),
      name = randName,
      email = s"${randName}@noname.com",
      displayName = randName
    )
  }

  def mkUser: User = {
    val newUser = mkRandUser
    dbData.get.add(newUser).commit
    newUser
  }

  def update(id: String, user: User): Unit = {
    dbData.get.remove(id).add(user).commit
  }

  def list: Iterable[User] = {
    dbData.get.userMap.values
  }
}
