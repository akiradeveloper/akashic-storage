package akasha.model

import java.text.SimpleDateFormat
import java.util.{TimeZone, Date}

object Dates {
  def format000Z(date: Date): String = {
    val sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"))
    sdf.format(date)
  }
}
