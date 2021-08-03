package org.scalawiki.query

import org.scalawiki.dto.Page
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.prop.{CategoryInfo, Prop}
import org.scalawiki.dto.cmd.query.{Query, TitlesParam}
import org.scalawiki.util.{HttpStub, MockBotSpec}
import org.specs2.mutable.Specification
import spray.util.pimpFuture

class ListCategoryMembersSpec extends Specification with MockBotSpec {

  "get category members with continue" should {
    "return category members" in {
      val queryType = "categorymembers"

      val response1 =
        """{ "query":
          | { "categorymembers": [{ "pageid": 569559, "ns": 1, "title": "Talk:Welfare reform" }] },
          | "continue": { "continue": "-||", "cmcontinue": "10|Stub|6674690" }}""".stripMargin

      val response2 =
        """{ "limits": {"categorymembers": 500}, "query":
          | { "categorymembers": [{"pageid": 4571809, "ns": 2, "title": "User:Formator"}] }}""".stripMargin

      val commands = Seq(
        HttpStub(Map("action" -> "query", "list" -> queryType, "cmlimit" -> "max",
          "cmtitle" -> "Category:SomeCategory", "cmnamespace" -> "", "continue" -> ""), response1),
        HttpStub(Map("action" -> "query", "list" -> queryType, "cmlimit" -> "max",
          "cmtitle" -> "Category:SomeCategory", "cmnamespace" -> "",
          "continue" -> "-||", "cmcontinue" -> "10|Stub|6674690"), response2)
      )

      val bot = getBot(commands: _*)

      val result = bot.page("Category:SomeCategory").categoryMembers().await
      result must have size 2
      result(0) === Page(569559, Some(1), "Talk:Welfare reform")
      result(1) === Page(4571809, Some(2), "User:Formator")
    }
  }

  "get category number" should {
    "return category number for 3 entries and missing entry" in {
      val queryType = "categorymembers"

      val response1 =
        """{
          |    "batchcomplete": "",
          |    "query": {
          |        "pages": {
          |            "-1": {
          |                "ns": 0,
          |                "title": "NoSuchTitle",
          |                "missing": ""
          |            },
          |            "736": {
          |                "pageid": 736,
          |                "ns": 0,
          |                "title": "Albert Einstein"
          |            },
          |            "50177636": {
          |                "pageid": 50177636,
          |                "ns": 14,
          |                "title": "Category:Foo",
          |                "categoryinfo": {
          |                    "size": 5,
          |                    "pages": 3,
          |                    "files": 2,
          |                    "subcats": 0
          |                }
          |            },
          |            "3108204": {
          |                "pageid": 3108204,
          |                "ns": 14,
          |                "title": "Category:Infobox templates",
          |                "categoryinfo": {
          |                    "size": 29,
          |                    "pages": 15,
          |                    "files": 0,
          |                    "subcats": 14
          |                }
          |            }
          |        }
          |    }
          |}""".stripMargin


      val commands = Seq(
        HttpStub(Map("action" -> "query", "prop" -> "categoryinfo",
          "titles" -> "Albert Einstein|Category:Foo|Category:Infobox_templates|NoSuchTitle", "continue" -> ""), response1)
      )

      val bot = getBot(commands: _*)

      val query = Action(Query(TitlesParam(Seq("Albert Einstein", "Category:Foo", "Category:Infobox_templates", "NoSuchTitle")),
        Prop(CategoryInfo)))

      val result = bot.run(query).await
      result must have size 4
      result(0) === new Page(None, Some(0), "NoSuchTitle", missing = true)
      result(1) === Page(736, Some(0), "Albert Einstein")
      result(2) === Page(50177636, Some(14), "Category:Foo").copy(
        categoryInfo = Some(org.scalawiki.dto.CategoryInfo(5, 3, 2, 0))
      )
      result(3) === Page(3108204, Some(14), "Category:Infobox templates").copy(
        categoryInfo = Some(org.scalawiki.dto.CategoryInfo(29, 15, 0, 14))
      )
    }
  }

}
