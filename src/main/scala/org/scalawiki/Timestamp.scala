package org.scalawiki

import org.joda.time.DateTime

object Timestamp {

  val pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'"

  val df = org.joda.time.format.DateTimeFormat.forPattern(pattern)

  def parse(input: String) = DateTime.parse(input, df)

  def format(dt: DateTime) = dt.toString(df)

}
