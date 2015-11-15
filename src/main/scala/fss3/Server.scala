package fss3

import com.twitter.finagle.Http
import com.twitter.util.Await
import io.finch._

case class Server(config: ServerConfig) {
  val tree = config.treePath
  val admin = config.adminPath

  val callerId = None

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
