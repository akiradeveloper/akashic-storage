package fss3

import com.twitter.finagle.Http
import com.twitter.finagle.http.Status
import com.twitter.util.Await
import io.finch._

import scala.xml.XML

case class Server(config: ServerConfig) {
  val tree = config.treePath
  val admin = config.adminPath

  val users = UserTable(admin)

  val callerId = Some(TestUsers.hoge.id)

  val adminService =
    get("admin" / "user") {
      val newUser = users.mkUser
      Ok(User.toXML(newUser))
    } :+:
    get("admin" / "user" / string) { id: String =>
      val user = users.getUser(id)
      user.isDefined.orFailWith(Error.AccountProblem()) // FIXME error type
      Ok(User.toXML(user.get))
    } :+:
    delete("admin" / "user" / string) { id: String =>
      Output.Payload("", Status.NotImplemented)
    } :+:
    put("admin" / "user" / string ? request.body) { (id: String, body: String) =>
      val xml = XML.loadString(body)
      val user = users.getUser(id)
      user.isDefined.orFailWith(Error.AccountProblem())
      val newUser = user.get.modifyWith(xml)
      users.updateUser(id, newUser)
      Ok("")
    }

  val doGetService: Endpoint[String] = get(/) {
    val xml = <a/>
    xml.toString
    // "hoge"
    // <ListAllMyBucketsResult>
    //   <Owner>
    //     <ID>{callerId.get}</ID>
    //     <DisplayName>{admin.getUser(callerId.get).get.displayName}</DisplayName>
    //   </Owner>
    //   <Buckets>
    //     { for (b <- tree.listBuckets if b.completed) yield
    //   <Bucket>
    //     <Name>{b.path.lastName}</Name>
    //     <CreationDate>{b.path.lastModified.format000Z}</CreationDate>
    //   </Bucket>
    //     }
    //   </Buckets>
    // </ListAllMyBucketsResult>
  }

  val api: Endpoint[String] =
    doGetService
    // :+: doGetBucket
    // :+: doGetObject
    // :+: doGetObject
    // :+: doPutObject
    // :+: doPutBucket

  val TMPRESOURCE = "/"
  val TMPREQID = "TMPREDID"

  val endpoint = api.handle {
    case Error.Exception(e) =>
      val cam = Error.toCodeAndMessage(e)
      val xml = Error.mkXML(cam, TMPRESOURCE, TMPREQID)
      Output.Payload(xml.toString)
  }
}

object TestApp extends App {
  val config = ServerConfig.forTest
  Await.ready(Http.server.serve(s"${config.ip}:${config.port}", Server(config).endpoint.toService))
}
