package akashic.http

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
    put("admin" / "user" / string ? body) { (id: String, body: String) =>
      val xml = XML.loadString(body)
      val user = users.getUser(id)
      user.isDefined.orFailWith(Error.AccountProblem())
      val newUser = user.get.modifyWith(xml)
      users.updateUser(id, newUser)
      Ok("")
    }


  val TMPRESOURCE = "/"
  val TMPREQID = "TMPREQID"

  val doGetService = get(/) {
    val xml: NodeSeq =
     <ListAllMyBucketsResult>
       <Owner>
         <ID>{callerId.get}</ID>
         <DisplayName>{users.getUser(callerId.get).get.displayName}</DisplayName>
       </Owner>
       <Buckets>
         { for (b <- tree.listBuckets if b.completed) yield
       <Bucket>
         <Name>{b.path.lastName}</Name>
         <CreationDate>{b.path.lastModified.format000Z}</CreationDate>
       </Bucket>
         }
       </Buckets>
     </ListAllMyBucketsResult>
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
