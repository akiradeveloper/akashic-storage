package akasha.admin

import java.nio.file.{FileSystems, Path}

import scala.slick.driver.SQLiteDriver.simple._
import scala.util.Random

case class UserTableDef(tag: Tag) extends Table[User](tag, "USER") {
  def id = column[String]("ID", O.PrimaryKey)
  def accessKey = column[String]("ACCESSKEY")
  def secretKey = column[String]("SECRETKEY")
  def name = column[String]("NAME")
  def email = column[String]("EMAIL")
  def displayName = column[String]("DISPLAYNAME")
  def * = (id, accessKey, secretKey, name, email, displayName) <>((User.apply _).tupled, User.unapply _)
}

case class UserTable(path: Path) {

  val users = TableQuery[UserTableDef]

  private val db = {
    val url = path.toString
    val ret = Database.forURL(s"jdbc:sqlite:${url}", driver = "org.sqlite.JDBC")
    if (!path.exists) {
      ret withSession { implicit session =>
        users.ddl.create
      }
    }
    ret
  }

  private def randStr(n: Int) = {
    Random.alphanumeric.take(n).mkString
  }

  private def mkRandUser: User = {
    User(
      id = randStr(64),
      accessKey = randStr(20).toUpperCase,
      secretKey = randStr(40),
      name = "noname",
      email = "noname@noname.org",
      displayName = "noname"
    )
  }

  def addUser(user: User): Unit = {
    db withSession { implicit session =>
      users.insert(user)
    }
  }

  def mkUser: User = {
    db withSession { implicit session =>
      val newUser = mkRandUser
      users.insert(newUser)
      newUser
    }
  }

  def getId(accessKey: String): Option[String] = {
    db.withSession { implicit session =>
      users.where(_.accessKey === accessKey).list.headOption.map(_.id)
    }
  }

  def getUser(id: String): Option[User] = {
    db.withSession { implicit session =>
      users.where(_.id === id).list.headOption
    }
  }

  // TODO findUser that throws exception on not found

  def updateUser(id: String, user: User): Unit = {
    db withSession { implicit session =>
      users.where(_.id === id).update(user)
    }
  }
  // def delUser: User = { }
}
