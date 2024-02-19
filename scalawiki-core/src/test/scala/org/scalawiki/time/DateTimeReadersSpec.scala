package org.scalawiki.time

import java.time.format.DateTimeParseException
import java.time.{Instant, ZoneOffset, ZonedDateTime}

import com.typesafe.config.{ConfigException, ConfigFactory}
import net.ceedubs.ficus.Ficus._
import org.specs2.mutable.Specification

class DateTimeReadersSpec extends Specification {
  val config = ConfigFactory.parseString("""
    num = 123
    str = "2013-01-05T12:00:00Z"
    invalid = "invalid"
    """)

  "DateTimeReader" should {
    "read number value to ReadableInstant (DateTime)" in {
      config.as[ZonedDateTime]("num") must throwA[DateTimeParseException]
    }
  }

  "DateTimeReader" should {
    "read iso-8601 string value to ReadableInstant (DateTime)" in {
      config.as[ZonedDateTime]("str") === ZonedDateTime.parse(
        "2013-01-05T12:00:00Z"
      )
      config.as[Option[ZonedDateTime]]("str") === Option(
        ZonedDateTime.parse("2013-01-05T12:00:00Z")
      )
      config.as[ZonedDateTime]("invalid") must throwA[DateTimeParseException]
    }
  }
}
