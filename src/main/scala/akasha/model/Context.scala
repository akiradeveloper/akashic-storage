package akasha.model

import akasha.admin.UserTable
import akasha.patch.Tree

case class Context(tree: Tree, users: UserTable, requestId: String, callerId: Option[String], resource: String)
extends GetService {
  def failWith(e: Error.t) {
    throw Error.Exception(this, e)
  }
}
