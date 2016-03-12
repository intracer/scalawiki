package org.scalawiki.query

import java.util.concurrent.TimeUnit

import org.scalawiki.dto.Page
import org.scalawiki.util.{Command, MockBotSpec}
import org.specs2.mutable.Specification

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class PropEmbeddedInSpec extends Specification with MockBotSpec {

  "get no embedded in" should {
    "return empty seq" in {
      val queryType = "embeddedin"

      //      val resonse = {"query-continue":{"embeddedin":{"eicontinue":"10|Stub|6674690"}},"query":{"embeddedin":[{"pageid":569559,"ns":1,"title":"Talk:Welfare reform"},{"pageid":2581310,"ns":2,"title":"User:Rpyle731/sandbox/archive1"},{"pageid":3954860,"ns":3,"title":"User talk:PBS/Archive 6"},{"pageid":4571809,"ns":2,"title":"User:Formator"},{"pageid":5024711,"ns":3,"title":"User talk:Rauterkus"}]}}

      val response = """{"query":{"embeddedin":[]}}"""

      val bot = getBot(new Command(Map("action" -> "query", "list" -> queryType, "eilimit" -> "max", "eititle" -> "Template:SomeTemplate",
        "einamespace" -> "", "continue" -> ""), response))

      val future = bot.page("Template:SomeTemplate").whatTranscludesHere()
      val result = Await.result(future, Duration(2, TimeUnit.SECONDS))
      result must have size 0
    }
  }

  "get one embedded in" should {
    "return one embedded in" in {
      val queryType = "embeddedin"

      //      val resonse = {"query-continue":{"embeddedin":{"eicontinue":"10|Stub|6674690"}},"query":{"embeddedin":[{"pageid":569559,"ns":1,"title":"Talk:Welfare reform"},{"pageid":2581310,"ns":2,"title":"User:Rpyle731/sandbox/archive1"},{"pageid":3954860,"ns":3,"title":"User talk:PBS/Archive 6"},{"pageid":4571809,"ns":2,"title":"User:Formator"},{"pageid":5024711,"ns":3,"title":"User talk:Rauterkus"}]}}

      val response = """{"limits": {"embeddedin": 500}, "query":{"embeddedin":[{"pageid":569559,"ns":1,"title":"Talk:Welfare reform"}]}}"""

      val bot = getBot(new Command(Map("action" -> "query", "list" -> queryType, "eilimit" -> "max", "eititle" -> "Template:SomeTemplate",
        "einamespace" -> "", "continue" -> ""), response))

      val future = bot.page("Template:SomeTemplate").whatTranscludesHere()
      val result = Await.result(future, Duration(2, TimeUnit.SECONDS))
      result must have size 1
      result(0) === Page(569559, 1, "Talk:Welfare reform")
    }
  }

  "get two embedded in" should {
    "return two embedded in" in {
      val queryType = "embeddedin"

      val response =
        """{"limits": {"embeddedin": 500}, "query":{"embeddedin":[
          |{"pageid":569559,"ns":1,"title":"Talk:Welfare reform"},
          |{"pageid":4571809,"ns":2,"title":"User:Formator"}]}}""".stripMargin

      val bot = getBot(new Command(Map("action" -> "query", "list" -> queryType, "eilimit" -> "max", "eititle" -> "Template:SomeTemplate",
        "einamespace" -> "", "continue" -> ""), response))

      val future = bot.page("Template:SomeTemplate").whatTranscludesHere()
      val result = Await.result(future, Duration(2, TimeUnit.SECONDS))
      result must have size 2
      result(0) === Page(569559, 1, "Talk:Welfare reform")
      result(1) === Page(4571809, 2, "User:Formator")
    }
  }

  "get embedded in with continue" should {
    "return one embedded in" in {
      val queryType = "embeddedin"

      //      val resonse = {"query-continue":{"embeddedin":{"eicontinue":"10|Stub|6674690"}},"query":{"embeddedin":[{"pageid":569559,"ns":1,"title":"Talk:Welfare reform"},{"pageid":2581310,"ns":2,"title":"User:Rpyle731/sandbox/archive1"},{"pageid":3954860,"ns":3,"title":"User talk:PBS/Archive 6"},{"pageid":4571809,"ns":2,"title":"User:Formator"},{"pageid":5024711,"ns":3,"title":"User talk:Rauterkus"}]}}

      val response1 = """{"query":{"embeddedin":[{"pageid":569559,"ns":1,"title":"Talk:Welfare reform"}]}, "continue":{"continue":"-||","eicontinue":"10|Stub|6674690"}}"""
      val response2 = """{"limits": {"embeddedin": 500}, "query":{"embeddedin":[ {"pageid":4571809,"ns":2,"title":"User:Formator"}]}}"""

      val commands = Seq(
        new Command(Map("action" -> "query", "list" -> queryType, "eilimit" -> "max", "eititle" -> "Template:SomeTemplate",
          "einamespace" -> "", "continue" -> ""), response1),
        new Command(Map("action" -> "query", "list" -> queryType, "eilimit" -> "max", "eititle" -> "Template:SomeTemplate",
          "einamespace" -> "", "continue" -> "-||", "eicontinue" -> "10|Stub|6674690"), response2)
      )

      val bot = getBot(commands:_*)

      val future = bot.page("Template:SomeTemplate").whatTranscludesHere()
      val result = Await.result(future, Duration(2, TimeUnit.SECONDS))
      result must have size 2
      result(0) === Page(569559, 1, "Talk:Welfare reform")
      result(1) === Page(4571809, 2, "User:Formator")
    }
  }
}
