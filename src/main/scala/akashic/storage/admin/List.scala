package akashic.storage.admin

import akka.http.scaladsl.server.Directives._
import akashic.storage.server
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._

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

