package org.scalawiki

import org.joda.time.DateTime

object Timestamp {

  val pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'"

  val df = org.joda.time.format.DateTimeFormat.forPattern(pattern).withZoneUTC()

  // TODO org.joda.time.IllegalInstantException: Cannot parse "2015-03-29T03:35:04Z": Illegal instant due to time zone offset transition (Europe/Kiev)
  def parse(input: String) = df.parseDateTime(input)

  def format(dt: DateTime) = dt.toString(df)

}
