package akasha.service

import akasha.admin.TestUsers
import io.finch.RequestReader

object CallerId {
  val TMPCALLERID = TestUsers.hoge.id
  val reader: RequestReader[String] = RequestReader.value(TMPCALLERID)
}
