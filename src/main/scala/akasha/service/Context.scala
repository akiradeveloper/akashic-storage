package akasha.service

import akasha.admin.UserTable
import akasha.patch.Tree

case class Context(tree: Tree, users: UserTable, requestId: String, callerId: String, resource: String)
extends Object
with GetService
with PutBucket {
  def failWith(e: Error.t) {
    throw Error.Exception(this, e)
  }
}
