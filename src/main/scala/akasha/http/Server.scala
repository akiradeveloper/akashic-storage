package akasha.http

import akasha.admin
import akasha.admin._
import akasha.model
import akasha.model._
import akasha.patch.Tree
import com.twitter.finagle.{ListeningServer, Http}
import com.twitter.finagle.http.Status
import com.twitter.util.Await
import io.finch._

import scala.xml.NodeSeq

case class Server(config: ServerConfig) {
  val tree = Tree(config.treePath)
  val users = UserTable(config.adminPath.resolve("db.sqlite"))
  val TMPREQID = "TMPREQID"
  val TMPCALLERID = TestUsers.hoge.id
  val TMPRESOURCE = "/"

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

  val TMPCONTEXT = Context(tree, users, TMPREQID, TMPCALLERID, TMPRESOURCE)

  val readContext = for {
    reqid <- RequestReader.value(TMPREQID)
    callerid <- RequestReader.value(TMPCALLERID)
    resource <- RequestReader.value(TMPRESOURCE)
  } yield Context(tree, users, reqid, callerid, resource)

  object GetService {
    val readParams = get(/).as[model.GetService.Input]
  }

  val doGetService = GetService.readParams { input: model.GetService.Input =>
    val model.GetService.Output(xml) = TMPCONTEXT.doGetService(input)
    Ok(xml)
      .withHeader(("x-amz-request-id", TMPREQID))
  }

  val doGetBucket = get(string) { bucketName: String =>
    Ok("hoge")
  }

  val doGetObject = get(string / string) { (bucketName: String, keyName: String) =>
    Ok("hoge")
  }

  object PutBucket {
    // val readParams = for {
    //   bucketName <- put(string)
    //   context <- readContext
    // } yield model.PutBucket.Input(bucketName)
    val readParams = put(string) ? readContext ? readContext
  }

  val doPutBucket = PutBucket.readParams { (bucketName: String, context: Context, context2: Context) =>
    val model.PutBucket.Output() = TMPCONTEXT.doPutBucket(model.PutBucket.Input(bucketName))
    Ok()
      .withHeader(("x-amz-request-id", TMPREQID))
  }

  object PutObject {
    val readParams = get(string / string ? binaryBody)
  }

  val doPutObject = PutObject.readParams { (bucketName: String, keyName: String, body: Array[Byte]) =>
    Ok("hoge")
  }

  val api =
    adminService :+:
    doGetService :+:
    doGetBucket :+:
    doGetObject :+:
    doPutObject :+:
    doPutBucket

  val endpoint = api.handle {
    case model.Error.Exception(context, e) =>
      val withMessage = model.Error.withMessage(e)
      val xml = model.Error.mkXML(withMessage, context.resource, context.requestId)
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
