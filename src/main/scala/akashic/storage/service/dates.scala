package akashic.storage.service

import java.text.SimpleDateFormat
import java.util.{Date, Locale, TimeZone}

object dates {
  def format000Z(date: Date): String = {
    val sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH)
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"))
    sdf.format(date)
  }
  // example: Fri, 22 Jan 2016 04:42:34 GMT
  def formatLastModified(date: Date): String = {
    val sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH)
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"))
    sdf.format(date)
  }
}
