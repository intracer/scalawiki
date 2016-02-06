package org.scalawiki.query

import java.util.concurrent.TimeUnit

import org.scalawiki.dto.Namespace
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.list.ListArgs
import org.scalawiki.dto.cmd.query.{Generator, Query}
import org.scalawiki.dto.cmd.query.prop.{LlLimit, LangLinks, Prop}
import org.scalawiki.util.{Command, MockBotSpec}
import org.specs2.mutable.Specification

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class PropLangLinksSpec extends Specification with MockBotSpec {

  "get lang links" should {
    "merge pages" in {

      //        "https://en.wikipedia.org/w/api.php?gcmtitle=Category:Cities_of_district_significance_in_Ukraine&format=json&generator=categorymembers&gcmlimit=10&lllimit=10&gcmnamespace=0&prop=langlinks&action=query&continue="

      val category = "Category:Cities_of_district_significance_in_Ukraine"

      val response1 =
        """|{
          |  "continue": {
          |    "llcontinue": "6863578|cs",
          |    "continue": "||"
          |  },
          |  "query": {
          |    "pages": {
          |      "6863578": {
          |        "pageid": 6863578,
          |        "ns": 0,
          |        "title": "Almazna",
          |        "langlinks": [
          |          {
          |            "lang": "ar",
          |            "*": "ألمازنا"
          |          },
          |          {
          |            "lang": "crh",
          |            "*": "Almazna"
          |          }
          |        ]
          |      },
          |      "45246116": {
          |        "pageid": 45246116,
          |        "ns": 0,
          |        "title": "City of district significance (Ukraine)"
          |      }
          |    }
          |  }
          |}
        """.stripMargin

      val response2 =
        """{
          |  "query": {
          |    "pages": {
          |      "6863578": {
          |        "pageid": 6863578,
          |        "ns": 0,
          |        "title": "Almazna",
          |        "langlinks": [
          |          {
          |            "lang": "cs",
          |            "*": "Almazna"
          |          },
          |          {
          |            "lang": "de",
          |            "*": "Almasna"
          |          }
          |        ]
          |      },
          |      "45246116": {
          |        "pageid": 45246116,
          |        "ns": 0,
          |        "title": "City of district significance (Ukraine)"
          |      }
          |    }
          |  }
          |}
        """.stripMargin

      val commands = Seq(
        new Command(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> category, "gcmnamespace" -> "0", "gcmlimit" -> "2", "lllimit" -> "2",
          "prop" -> "langlinks",
          "continue" -> ""), response1),
        new Command(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> category, "gcmnamespace" -> "0", "gcmlimit" -> "2", "lllimit" -> "2",
          "prop" -> "langlinks",
          "continue" -> "||",
          "llcontinue" -> "6863578|cs"),
          response2)
      )

      val bot = getBot(commands: _*)

      val action = Action(Query(
        Prop(
          LangLinks(LlLimit("2"))
        ),
        Generator(ListArgs.toDsl("categorymembers", Some(category), None, Set(Namespace.MAIN), Some("2")))
      ))

      val future = new DslQuery(action, bot).run()

      val result = Await.result(future, Duration(2, TimeUnit.SECONDS))
      result must have size 2
      val p1 = result.head
      p1.id === Some(6863578)
      p1.title === "Almazna"
      p1.langLinks.size === 4
      p1.langLinks === Map(
        "ar" -> "ألمازنا",
        "crh" -> "Almazna",
        "cs" -> "Almazna",
        "de" -> "Almasna"
      )

      val p2 = result.last
      p2.id === Some(45246116)
      p2.title === "City of district significance (Ukraine)"
    }
  }


}
