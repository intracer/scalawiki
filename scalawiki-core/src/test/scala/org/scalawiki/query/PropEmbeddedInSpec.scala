package org.scalawiki.query

import org.scalawiki.dto.Page
import org.scalawiki.util.{HttpStub, MockBotSpec}
import org.specs2.mutable.Specification
import spray.util.pimpFuture

import scala.concurrent.ExecutionContext.Implicits.global

class PropEmbeddedInSpec extends Specification with MockBotSpec {

  "get no embedded in" should {
    "return empty seq" in {
      val queryType = "embeddedin"

      val response = """{ "query": { "embeddedin": [] }}"""

      val query = Map(
        "action" -> "query",
        "list" -> queryType,
        "eilimit" -> "max",
        "eititle" -> "Template:SomeTemplate",
        "einamespace" -> "",
        "continue" -> ""
      )
      val bot = getBot(HttpStub(query, response))

      val result = bot.page("Template:SomeTemplate").whatTranscludesHere().await
      result must have size 0
    }
  }

  "get one embedded in" should {
    "return one embedded in" in {
      val queryType = "embeddedin"

      val response =
        """{ "query": { "embeddedin":
           [{ "pageid": 569559, "ns": 1, "title": "Talk:Welfare reform"}]
          }}"""

      val query = Map(
        "action" -> "query",
        "list" -> queryType,
        "eilimit" -> "max",
        "eititle" -> "Template:SomeTemplate",
        "einamespace" -> "",
        "continue" -> ""
      )

      val bot = getBot(HttpStub(query, response))

      val result = bot.page("Template:SomeTemplate").whatTranscludesHere().await
      result must have size 1
      result.head === Page(569559, Some(1), "Talk:Welfare reform")
    }
  }

  "get two embedded in" should {
    "return two embedded in" in {
      val queryType = "embeddedin"

      val response =
        """{ "query": { "embeddedin": [
          { "pageid": 569559, "ns": 1, "title": "Talk:Welfare reform" },
          { "pageid": 4571809, "ns": 2, "title": "User:Formator" }
          ]}}"""

      val query = Map(
        "action" -> "query",
        "list" -> queryType,
        "eilimit" -> "max",
        "eititle" -> "Template:SomeTemplate",
        "einamespace" -> "",
        "continue" -> ""
      )

      val bot = getBot(HttpStub(query, response))

      val result = bot
        .page("Template:SomeTemplate")
        .whatTranscludesHere()
        .map(_.toSeq)
        .await
      result must have size 2
      result(0) === Page(569559, Some(1), "Talk:Welfare reform")
      result(1) === Page(4571809, Some(2), "User:Formator")
    }
  }

  "get embedded in with continue" should {
    "return one embedded in" in {
      val queryType = "embeddedin"

      val response1 =
        """{"query": {
           "embeddedin":
           [{ "pageid": 569559, "ns": 1, "title": "Talk:Welfare reform" }] },
           "continue":
           { "continue":"-||", "eicontinue": "10|Stub|6674690" }
          }"""
      val response2 =
        """{ "query": { "embeddedin": [{"pageid": 4571809, "ns": 2, "title": "User:Formator" }] }}"""

      val query = Map(
        "action" -> "query",
        "list" -> queryType,
        "eilimit" -> "max",
        "eititle" -> "Template:SomeTemplate",
        "einamespace" -> ""
      )

      val commands = Seq(
        HttpStub(query + ("continue" -> ""), response1),
        HttpStub(
          query ++ Map("continue" -> "-||", "eicontinue" -> "10|Stub|6674690"),
          response2
        )
      )

      val bot = getBot(commands: _*)

      val result = bot
        .page("Template:SomeTemplate")
        .whatTranscludesHere()
        .map(_.toSeq)
        .await
      result must have size 2
      result(0) === Page(569559, Some(1), "Talk:Welfare reform")
      result(1) === Page(4571809, Some(2), "User:Formator")
    }
  }
}
