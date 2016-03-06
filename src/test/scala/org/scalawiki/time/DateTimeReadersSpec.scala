package org.scalawiki.time


import com.typesafe.config.{ConfigException, ConfigFactory}
import net.ceedubs.ficus.Ficus._
import org.joda.time._
import org.specs2.mutable.Specification
import org.scalawiki.time.imports._

class DateTimeReadersSpec extends Specification {
  val config = ConfigFactory.parseString(
    """
    num = 123
    str = "2013-01-05T12:00:00Z"
    invalid = "invalid"
    """)

  "DateTimeReader" should {
    "read number value to ReadableInstant (DateTime)" in {
      config.as[DateTime]("num") === new DateTime(123L)
    }
  }

  "DateTimeReader" should {
    "read iso-8601 string value to ReadableInstant (DateTime)" in {
      config.as[DateTime]("str") === new DateTime("2013-01-05T12:00:00Z")
      config.as[Option[DateTime]]("str") === Option(new DateTime("2013-01-05T12:00:00Z"))
      config.as[DateTime]("invalid") must throwA[ConfigException.BadValue]("Invalid format")
    }
  }
}