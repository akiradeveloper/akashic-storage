package akasha.http

import com.twitter.finagle.Http
import com.twitter.finagle.http.Status
import com.twitter.util.{Future, Await}
import io.finch._

import io.finch.ext._

import scala.xml.{NodeSeq, XML}

case class Server(config: ServerConfig) {
  val tree = Tree(config.treePath)
  val users = UserTable(config.adminPath)

  val callerId = Some(TestUsers.hoge.id)

  val adminService =
    get("admin" / "user") {
      val result = MakeUser.run(users)
      Ok(result.xml)
    } :+:
    get("admin" / "user" / string) { id: String =>
      val result = GetUser.run(users, id)
      Ok(result.xml)
    } :+:
    delete("admin" / "user" / string) { id: String =>
      Output.Payload("", Status.NotImplemented)
    } :+:
    put("admin" / "user" / string ? body) { (id: String, body: String) =>
      UpdateUser.run(users, id, body)
      Ok("")
    }

  val TMPRESOURCE = "/"
  val TMPREQID = "TMPREQID"

  val doGetService = get(/) {
    val xml = ???
    Ok(xml)
      .withHeader(("x-amz-request-id", TMPREQID))
  }

  val doGetBucket = get(string) { bucketName: String =>
    Ok("hoge")
  }

  val api =
    adminService :+:
    doGetService
    // :+: doGetBucket
    // :+: doGetObject
    // :+: doGetObject
    // :+: doPutObject
    // :+: doPutBucket


  val endpoint = api.handle {
    //case e => BadRequest(io.finch.Error("aaa"))
    case Error.Exception(e) =>
      val cam = Error.toCodeAndMessage(e)
      val xml = Error.mkXML(cam, TMPRESOURCE, TMPREQID)
      BadRequest(io.finch.Error(xml.toString)).withHeader(("a", "b"))
  }
}

object TestApp extends App {
  val config = ServerConfig.forTest
  Await.ready(Http.server.serve(s"${config.ip}:${config.port}", Server(config).endpoint.toService))
}
