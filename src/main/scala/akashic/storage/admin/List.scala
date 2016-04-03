package akashic.storage.admin

import akashic.storage.server
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._
import akka.http.scaladsl.server.Directives._

object List {
  val matcher =
    get &
    path("admin" / "user")

  val route = matcher {
    authenticate {
      run(server.users)
    }
  }

  private def run(users: UserDB) = {
    val xml =
      <AdminListResult>
        { for (user <- users.list) yield <UserId>{user.id}</UserId> }
      </AdminListResult>
    complete(xml)
  }
}

