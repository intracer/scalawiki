package org.scalawiki.bot

import java.time.{ZoneOffset, ZonedDateTime}

import com.typesafe.config.ConfigFactory
import org.scalawiki.bots.{Message, MessageBot}
import org.scalawiki.time.TimeRange
import org.specs2.mutable.Specification

class MessageBotSpec extends Specification {

  val cfgStr =
    """message = {
         host = "uk.wikipedia.org"
         users = {
           list = "Wikipedia:User signatures"
           start = "2016-01-29"
           end = "2016-02-01"
         }
         talk-page = {
           subject = "talk page subject"
           body = "talk page body"
         }
         email = {
           subject = "mail subject"
           body = "mail body"
         }
       }"""

  val defaultConfig = ConfigFactory.parseString(cfgStr).getConfig("message")

  "configs" should {
    "read fully" in {

      val bot = new MessageBot(defaultConfig)

      bot.host === "uk.wikipedia.org"
      bot.userListPage === "Wikipedia:User signatures"
      bot.range === TimeRange(
        Some(ZonedDateTime.of(2016, 1, 29, 0, 0, 0, 0, ZoneOffset.UTC)),
        Some(ZonedDateTime.of(2016, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC))
      )

      bot.talkPageMessage === Message("talk page subject", "talk page body")
      bot.mail === Message("mail subject", "mail body")
    }

    "read no end in time range" in {

      val config = defaultConfig.withoutPath("users.end")

      val bot = new MessageBot(config)

      bot.range === TimeRange(Some(ZonedDateTime.of(2016, 1, 29, 0, 0, 0, 0, ZoneOffset.UTC)), None)
    }

  }

}
