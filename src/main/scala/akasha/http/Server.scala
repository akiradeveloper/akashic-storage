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
  val TMPREQID = "TMPREQID"
  val TMPCALLERID = Some(TestUsers.hoge.id)
  val TMPRESOURCE = "/"

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

  val TMPCONTEXT = Context(tree, users, TMPREQID, TMPCALLERID, TMPRESOURCE)

  val doGetService = get(/) {
    val xml = TMPCONTEXT.doGetService
    Ok(xml)
      .withHeader(("x-amz-request-id", TMPREQID))
  }

  val doGetBucket = get(string) { bucketName: String =>
    Ok("hoge")
  }

  val api =
    adminService :+:
    doGetService :+:
    doGetBucket
    // :+: doGetObject
    // :+: doGetObject
    // :+: doPutObject
    // :+: doPutBucket

  import com.twitter.finagle.http.Status
  val endpoint = api.handle {
    case Error.Exception(context, e) =>
      val withMessage = Error.withMessage(e)
      val xml = Error.mkXML(withMessage, context.resource, context.requestId)
      val cause = io.finch.Error(xml.toString)
      Output.Failure(cause, Status.fromCode(withMessage.httpCode))
        .withHeader(("a", "b"))
    case _ => assert(false) // TODO
  }
}

object TestApp extends App {
  val config = ServerConfig.forTest
  Await.ready(Http.server.serve(s"${config.ip}:${config.port}", Server(config).endpoint.toService))
}
