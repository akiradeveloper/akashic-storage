package akashic.storage.service

import akashic.storage.admin.TestUsers
import io.finch.RequestReader

object CallerId {
  val TMPCALLERID = TestUsers.hoge.id
  val reader: RequestReader[String] = RequestReader.value(TMPCALLERID)
}
