package org.scalawiki

import java.time.{ZoneOffset, ZonedDateTime}
import java.time.format.DateTimeFormatter

object Timestamp {

  val pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'"

  val df = DateTimeFormatter.ofPattern(pattern).withZone(ZoneOffset.UTC)

  // TODO org.joda.time.IllegalInstantException: Cannot parse "2015-03-29T03:35:04Z": Illegal instant due to time zone offset transition (Europe/Kiev)
  def parse(input: String) = ZonedDateTime.parse(input, df)

  def format(dt: ZonedDateTime) = dt.format(df)

}
