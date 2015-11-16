package fss3

import scala.xml.NodeSeq

case class User(id: String,
                accessKey: String,
                secretKey: String,
                name: String,
                email: String,
                displayName: String) {
  def modifyWith(xml: NodeSeq): User = {
    this.copy(
      name = (xml \ "Name").headOption.map(_.text).getOrElse(this.name),
      email = (xml \ "Email").headOption.map(_.text).getOrElse(this.email),
      displayName = (xml \ "DisplayName").headOption.map(_.text).getOrElse(this.displayName)
    )
  }
}

object User {
  def fromXML(xml: NodeSeq): User = {
    User(
      id = (xml \ "Id").text,
      accessKey = (xml \ "AccessKey").text,
      secretKey = (xml \ "SecretKey").text,
      name = (xml \ "Name").text,
      email = (xml \ "Email").text,
      displayName = (xml \ "DisplayName").text
    )
  }

  def toXML(user: User): NodeSeq = {
    <User>
      <Id>{user.id}</Id>
      <AccessKey>{user.accessKey}</AccessKey>
      <SecretKey>{user.secretKey}</SecretKey>
      <Name>{user.name}</Name>
      <Email>{user.email}</Email>
      <DisplayName>{user.displayName}</DisplayName>
    </User>
  }
}
