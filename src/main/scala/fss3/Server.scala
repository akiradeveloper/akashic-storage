package fss3

import io.finch._

class Server(config: ServerConfig) {
  val tree = config.treePath
  val admin = config.adminPath

  val endpoint =
    doGetService
    // :+: doGetBucket
    // :+: doGetObject
    // :+: doGetObject
    // :+: doPutObject
    // :+: doPutBucket

  val doGetService = get("/") {

  }
}
