package org.scalawiki.query

import java.util.concurrent.TimeUnit

import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.prop.{Links, Prop}
import org.scalawiki.dto.cmd.query.{Query, TitlesParam}
import org.scalawiki.util.{Command, MockBotSpec}
import org.specs2.mutable.Specification

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class PropLinksSpec extends Specification with MockBotSpec {

  "get links" should {
    "merge pages" in {

      val title = "Reactive Streams"   //  https://en.wikipedia.org/wiki/Reactive_Streams

      val response1 =
        """{
          |    "continue": {
          |        "plcontinue": "48896716|0|Domain-specific_language",
          |        "continue": "||"
          |    },
          |    "query": {
          |        "pages": {
          |            "48896716": {
          |                "pageid": 48896716,
          |                "ns": 0,
          |                "title": "Reactive Streams",
          |                "links": [
          |                    {
          |                        "ns": 0,
          |                        "title": "API"
          |                    },
          |                    {
          |                        "ns": 0,
          |                        "title": "Data buffer"
          |                    }
          |                ]
          |            }
          |        }
          |    }
          |}
        """.stripMargin

      val response2 =
        """|{
          |    "query": {
          |        "pages": {
          |            "48896716": {
          |                "pageid": 48896716,
          |                "ns": 0,
          |                "title": "Reactive Streams",
          |                "links": [
          |                    {
          |                        "ns": 0,
          |                        "title": "Implementation"
          |                    },
          |                    {
          |                        "ns": 0,
          |                        "title": "Interoperability"
          |                    }
          |                ]
          |            }
          |        }
          |    }
          |}""".stripMargin

      val commands = Seq(
        new Command(Map("action" -> "query",
          "titles" -> title,
          "prop" -> "links",
          "continue" -> ""), response1),
        new Command(Map("action" -> "query",
          "titles" -> title,
          "prop" -> "links",
          "continue" -> "||",
          "plcontinue" -> "48896716|0|Domain-specific_language"),
          response2)
      )

      val bot = getBot(commands: _*)

      val action = Action(Query(
        Prop(
          Links()
        ),
        TitlesParam(Seq(title))
      ))

      val future = bot.run(action)

      val result = Await.result(future, Duration(2, TimeUnit.SECONDS))
      result must have size 1
      val p1 = result.head
      p1.id === Some(48896716)
      p1.title === "Reactive Streams"
      p1.links.size === 4
      p1.links.map(_.title) === Seq("API",  "Data buffer", "Implementation",  "Interoperability")
      p1.links.map(_.ns).toSet === Set(0)
    }
  }
}
