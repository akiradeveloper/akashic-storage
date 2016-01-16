package akashic.storage

import java.nio.file.Files

import akashic.storage.admin._
import akashic.storage.service._
import akashic.storage.cleaner.{CleanerQueue, GarbageCan, TreeCompactor}
import akashic.storage.patch.Tree
import com.twitter.util.Future
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.{Http, ListeningServer, SimpleFilter, Service}
import com.twitter.finagle.transport.Transport
import io.finch._

import scala.xml.NodeSeq

case class Server(config: ServerConfig)
extends GetServiceSupport
with GetBucketSupport
with PutBucketSupport
with PutObjectSupport
with GetObjectSupport
with DeleteObjectSupport
with HeadBucketSupport
with InitiateMultipartUploadSupport
with UploadPartSupport
with CompleteMultipartUploadSupport {
  Files.createDirectory(config.mountpoint.resolve("tree"))
  val tree = Tree(config.mountpoint.resolve("tree"))

  Files.createDirectory(config.mountpoint.resolve("admin"))
  val users = UserTable(config.mountpoint.resolve("admin"))

  Files.createDirectory(config.mountpoint.resolve("garbage"))
  val garbageCan = GarbageCan(config.mountpoint.resolve("garbage"))

  // compact the store on reboot
  val cleanerQueue = CleanerQueue()
  cleanerQueue.queue(TreeCompactor(tree, this))

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
    GetBucket.endpoint :+:
    GetObject.endpoint :+:
    PutBucket.endpoint :+:
    PutObject.endpoint :+:
    DeleteObject.endpoint :+:
    HeadBucket.endpoint :+:
    InitiateMultipartUpload.endpoint :+:
    UploadPart.endpoint :+:
    CompleteMultipartUpload.endpoint

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

  def address = s"${config.ip}:${config.port}"

  def run: ListeningServer = {
    implicit val encodeXML: EncodeResponse[NodeSeq] = EncodeResponse.fromString("application/xml")(a => a.toString)
    val logFilter = new SimpleFilter[Request, Response] {
      def apply(req: Request, service: Service[Request, Response]): Future[Response] = {
        println(req)
        val res = service(req)
        res.onSuccess(println(_))
        res.onFailure(println(_))
        res
      }
    }
    val service = logFilter andThen this.endpoint.toService
    Http.server
      // .configured(Transport.Verbose(true))
      .serve(s"${address}", service)
  }
}
