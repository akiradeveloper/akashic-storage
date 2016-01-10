package akasha

import akasha.admin._
import akasha.service._
import akasha.patch.Tree
import com.twitter.finagle.http.Status
import com.twitter.finagle.{Http, ListeningServer}
import io.finch._

import scala.xml.NodeSeq

case class Server(config: ServerConfig)
extends GetServiceSupport
with PutBucketSupport
with PutObjectSupport
with GetObjectSupport {
  val tree = Tree(config.treePath)
  val users = UserTable(config.adminPath.resolve("db.sqlite"))

  val adminService =
    post("admin" / "user") {
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

  val api =
    adminService :+:
    GetService.endpoint :+:
    GetObject.endpoint :+:
    PutBucket.endpoint :+:
    PutObject.endpoint

  val endpoint = api.handle {
    case service.Error.Exception(context, e) =>
      val withMessage = service.Error.withMessage(e)
      val xml = service.Error.mkXML(withMessage, context.resource, context.requestId)
      val cause = io.finch.Error(xml.toString)
      Output.Failure(cause, Status.fromCode(withMessage.httpCode))
        .withHeader(("a", "b"))
    case admin.Error.Exception(e) =>
      val (code, message) = admin.Error.interpret(e)
      val cause = io.finch.Error(message)
      Output.Failure(cause, Status.fromCode(code))
  }

  def run: ListeningServer = {
    implicit val encodeXML: EncodeResponse[NodeSeq] = EncodeResponse.fromString("application/xml")(a => a.toString)
    Http.server.serve(s"${config.ip}:${config.port}", Server(config).endpoint.toService)
  }
}
