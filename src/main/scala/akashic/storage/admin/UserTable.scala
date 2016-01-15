package akashic.storage.admin

import java.nio.file.{Files, Path}

import akashic.storage.strings

import scala.slick.driver.SQLiteDriver.simple._

case class UserTableDef(tag: Tag) extends Table[User.t](tag, "USER") {
  def id = column[String]("ID", O.PrimaryKey)
  def accessKey = column[String]("ACCESSKEY")
  def secretKey = column[String]("SECRETKEY")
  def name = column[String]("NAME")
  def email = column[String]("EMAIL")
  def displayName = column[String]("DISPLAYNAME")
  def * = (id, accessKey, secretKey, name, email, displayName) <>((User.t.apply _).tupled, User.t.unapply _)
}

case class UserTable(root: Path) {
  val dbPath = root.resolve("db.sqlite")

  val users = TableQuery[UserTableDef]

  private val db = {
    val url = dbPath.toString
    val ret = Database.forURL(s"jdbc:sqlite:${url}", driver = "org.sqlite.JDBC")
    if (!Files.exists(dbPath)) {
      ret withSession { implicit session =>
        users.ddl.create
      }
    }
    ret
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

  def addUser(user: User.t): Unit = {
    db withSession { implicit session =>
      users.insert(user)
    }
  }

  def mkUser: User.t = {
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

  def getUser(id: String): Option[User.t] = {
    db.withSession { implicit session =>
      users.where(_.id === id).list.headOption
    }
  }

  def updateUser(id: String, user: User.t): Unit = {
    db withSession { implicit session =>
      users.where(_.id === id).update(user)
    }
  }
}