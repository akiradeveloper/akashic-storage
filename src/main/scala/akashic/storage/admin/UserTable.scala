package akashic.storage.admin

import java.nio.file.{Files, Path}

import akashic.storage.patch.{Data, Commit}
import akashic.storage.{files, strings}

import scala.pickling.Defaults._
import scala.pickling.binary._

case class UserTable(root: Path) {
  val dbPath = root.resolve("db")
  val dbData: Data = Data(dbPath)
  var updateTime: Long = -1L
  var db: InMem = InMem(Map(), Map())
  dbData.write(db.toByteArray)

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
      Commit.replaceData(dbData) { data: Data =>
        data.write(toByteArray)
      }
    }
  }

  private def reload = {
    val pTime = files.lastDate(dbPath).getTime
    // updateTime = -1L // WA to always reload
    if (updateTime < pTime) {
      val list = BinaryPickle(dbData.read).unpickle[Seq[User.t]]
      val userMap = list.map(a => (a.id, a)).toMap
      db = InMem(userMap.values)
      updateTime = pTime
    }
  }

  def getId(accessKey: String): Option[String] = {
    reload
    db.idMap.get(accessKey)
  }

  def getUser(id: String): Option[User.t] = {
    reload
    db.userMap.get(id)
  }

  def addUser(user: User.t): Unit = {
    reload
    db = db.add(user) // this is WA
    db.commit
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
    reload
    db.add(newUser).commit
    newUser
  }

  def updateUser(id: String, user: User.t): Unit = {
    reload
    db.remove(id).add(user).commit
  }
}
