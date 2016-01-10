package akasha.service
import io.finch.RequestReader

object RequestId {
  val TMPREQID = "TMPREQID"
  val reader = RequestReader.value(TMPREQID)
}
