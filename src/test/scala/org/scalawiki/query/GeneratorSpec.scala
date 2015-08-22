package org.scalawiki.query

import java.util.concurrent.TimeUnit

import org.scalawiki.Timestamp
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.list.ListArgs
import org.scalawiki.dto.cmd.query.prop.{LangLinks, LlLimit, Prop}
import org.scalawiki.dto.cmd.query.{Generator, Query}
import org.scalawiki.dto.{Namespace, Page, Revision, User}
import org.scalawiki.util.{Command, MockBotSpec}
import org.scalawiki.wlx.dto.Image
import org.specs2.mutable.Specification

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class GeneratorSpec extends Specification with MockBotSpec {

  "get revisions text in generator with continue" should {
    "return a page text" in {
      val pageText1 = "some vandalism"
      val pageText2 = "more vandalism"

      val response1 =
        s"""{"query": {"pages": {
          |"569559": {"pageid": 569559, "ns": 1, "title": "Talk:Welfare reform",
          |"revisions": [{"revid": 1, "user": "u1", "comment": "c1", "*": "$pageText1"}] }}},
          |"continue": {"continue": "gcmcontinue||", "gcmcontinue": "10|Stub|6674690"}}""".stripMargin

      val response2 =
        s"""{"query": {"pages": {
          |"4571809": {"pageid": 4571809, "ns": 2, "title": "User:Formator",
          |"revisions": [{"revid": 2, "user": "u2", "comment": "c2", "*": "$pageText2"}]} }}}""".stripMargin

      val commands = Seq(
        new Command(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "revisions", "rvprop" -> "ids|content|user|comment",
          "continue" -> ""), response1),
        new Command(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "revisions", "rvprop" -> "ids|content|user|comment",
          "continue" -> "gcmcontinue||", "gcmcontinue" -> "10|Stub|6674690"), response2)
      )

      val bot = getBot(commands: _*)

      val future = bot.page("Category:SomeCategory")
        .revisionsByGenerator("categorymembers", "cm", Set.empty, Set("ids", "content", "user", "comment"))

      val result = Await.result(future, Duration(2, TimeUnit.SECONDS))

      result must have size 2

      result(0) === Page(Some(569559L), 1, "Talk:Welfare reform",
        Seq(Revision(Some(1L), Some(569559L), None, Some(User(None, Some("u1"))), None, Some("c1"), Some(pageText1)))
      )
      result(1) === Page(Some(4571809L), 2, "User:Formator",
        Seq(Revision(Some(2L), Some(4571809L), None, Some(User(None, Some("u2"))), None, Some("c2"), Some(pageText2)))
      )
    }
  }

  "get image info in generator" should {
    "return a page text" in {

      val response1 =
        """  {
          |    "limits": {
          |      "categorymembers": 500
          |    },
          |    "query": {
          |      "pages": {
          |      "32885574": {
          |      "pageid": 32885574,
          |      "ns": 6,
          |      "title": "File:\"Dovbush-rocks\" 01.JPG",
          |      "imagerepository": "local",
          |      "imageinfo": [
          |    {
          |      "timestamp": "2014-05-20T20:54:33Z",
          |      "user": "Taras r",
          |      "size": 4270655,
          |      "width": 3648,
          |      "height": 2736,
          |      "url": "https://upload.wikimedia.org/wikipedia/commons/e/ea/%22Dovbush-rocks%22_01.JPG",
          |      "descriptionurl": "https://commons.wikimedia.org/wiki/File:%22Dovbush-rocks%22_01.JPG"
          |    }]}}},
          |    "continue": {
          |    "gcmcontinue": "file|44454d45524749373631312e4a50470a44454d45524749373631312e4a5047|32763876",
          |    "continue": "gcmcontinue||"}}
        """.stripMargin

      val response2 =
        """  {
          |    "query": {
          |      "pages": {
          |      "32885597": {
          |      "pageid": 32885597,
          |      "ns": 6,
          |      "title": "File:\"Dovbush-rocks\" 02.JPG",
          |      "imagerepository": "local",
          |      "imageinfo": [
          |    {
          |      "timestamp": "2014-05-20T20:55:12Z",
          |      "user": "Taras r",
          |      "size": 4537737,
          |      "width": 2736,
          |      "height": 3648,
          |      "url": "https://upload.wikimedia.org/wikipedia/commons/2/26/%22Dovbush-rocks%22_02.JPG",
          |      "descriptionurl": "https://commons.wikimedia.org/wiki/File:%22Dovbush-rocks%22_02.JPG"
          |    }]}}}}
          |    """.stripMargin


      val commands = Seq(
        new Command(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "imageinfo", "iiprop" -> "content|timestamp|user|comment",
          "continue" -> ""), response1),
        new Command(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "imageinfo", "iiprop" -> "content|timestamp|user|comment",
          "continue" -> "gcmcontinue||",
          "gcmcontinue" -> "file|44454d45524749373631312e4a50470a44454d45524749373631312e4a5047|32763876"),
          response2)
      )

      val bot = getBot(commands: _*)

      val future = bot.page("Category:SomeCategory")
        .imageInfoByGenerator("categorymembers", "cm", Set.empty, Set("content", "timestamp", "user", "comment"))

      val result = Await.result(future, Duration(2, TimeUnit.SECONDS))

      result must have size 2

      result(0) === Page(Some(32885574), 6, "File:\"Dovbush-rocks\" 01.JPG", Seq.empty,
        Seq(Image.basic("File:\"Dovbush-rocks\" 01.JPG", Some(Timestamp.parse("2014-05-20T20:54:33Z")), Some("Taras r"), Some(4270655), Some(3648), Some(2736),
          Some("https://upload.wikimedia.org/wikipedia/commons/e/ea/%22Dovbush-rocks%22_01.JPG"),
          Some("https://commons.wikimedia.org/wiki/File:%22Dovbush-rocks%22_01.JPG"), Some(32885574))))
      result(1) === Page(Some(32885597), 6, "File:\"Dovbush-rocks\" 02.JPG", Seq.empty,
        Seq(Image.basic("File:\"Dovbush-rocks\" 02.JPG", Some(Timestamp.parse("2014-05-20T20:55:12Z")), Some("Taras r"), Some(4537737), Some(2736), Some(3648),
          Some("https://upload.wikimedia.org/wikipedia/commons/2/26/%22Dovbush-rocks%22_02.JPG"),
          Some("https://commons.wikimedia.org/wiki/File:%22Dovbush-rocks%22_02.JPG"), Some(32885597))))
    }

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
            "generator" -> "categorymembers", "gcmtitle" -> category, "gcmlimit" -> "2", "lllimit" -> "2",
            "prop" -> "langlinks", "iiprop" -> "content|timestamp|user|comment",
            "continue" -> ""), response1),
          new Command(Map("action" -> "query",
            "generator" -> "categorymembers", "gcmtitle" -> category, "gcmlimit" -> "2", "lllimit" -> "2",
            "prop" -> "langlinks", "iiprop" -> "content|timestamp|user|comment",
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
}
