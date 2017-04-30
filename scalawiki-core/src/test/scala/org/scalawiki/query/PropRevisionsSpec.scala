package org.scalawiki.query

import org.scalawiki.dto.{Page, Revision, User}
import org.scalawiki.util.{HttpStub, MockBotSpec}
import org.specs2.mutable.Specification
import spray.util.pimpFuture

class PropRevisionsSpec extends Specification with MockBotSpec {

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
        new HttpStub(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "info|revisions", "rvprop" -> "ids|content|user|comment",
          "continue" -> ""), response1),
        new HttpStub(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "info|revisions", "rvprop" -> "ids|content|user|comment",
          "continue" -> "gcmcontinue||", "gcmcontinue" -> "10|Stub|6674690"), response2)
      )

      val bot = getBot(commands: _*)

      val future = bot.page("Category:SomeCategory")
        .revisionsByGenerator("categorymembers", "cm", Set.empty, Set("ids", "content", "user", "comment"))

      val result = future.await

      result must have size 2

      result(0) === Page(Some(569559L), 1, "Talk:Welfare reform",
        Seq(Revision(Some(1L), Some(569559L), None, Some(User(None, Some("u1"))), None, Some("c1"), Some(pageText1)))
      )
      result(1) === Page(Some(4571809L), 2, "User:Formator",
        Seq(Revision(Some(2L), Some(4571809L), None, Some(User(None, Some("u2"))), None, Some("c2"), Some(pageText2)))
      )
    }
  }

}
