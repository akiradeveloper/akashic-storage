package akasha.service

import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}

object DateOps {
  def format000Z(date: Date): String = {
    val sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"))
    sdf.format(date)
  }
}
