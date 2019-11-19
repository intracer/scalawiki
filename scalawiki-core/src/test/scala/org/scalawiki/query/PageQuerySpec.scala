package org.scalawiki.query


import akka.actor.ActorSystem
import org.scalawiki.MwBotImpl
import org.scalawiki.dto.{Page, Revision}
import org.scalawiki.util.{HttpStub, TestHttpClient}
import org.specs2.mutable.Specification

import scala.collection.mutable
import spray.util.pimpFuture

class PageQuerySpec extends Specification {

  val host = "uk.wikipedia.org"

  private val system: ActorSystem = ActorSystem()

  "get revisions text" should {
    "return a page text" in {
      val pageText1 = "some vandalism"
      val pageText2 = "more vandalism"

      val response =
        s"""{"query": { "pages": {
            "569559": { "pageid": 569559, "ns": 1, "title": "Talk:Welfare reform",
                        "revisions": [{ "revid": 1, "userid": 1, "user": "u1", "comment": "c1", "*": "$pageText1"}]},
            "4571809": { "pageid": 4571809, "ns": 2, "title": "User:Formator",
                          "revisions": [{ "revid": 2, "userid": 2, "user": "u2", "comment": "c2", "*": "$pageText2"}]}
            }}}"""

      val bot = getBot(HttpStub(
        Map(
          "pageids" -> "569559|4571809",
          "action" -> "query",
          "prop" -> "info|revisions",
          "continue" -> "", "rvlimit" -> "max",
          "rvprop" -> "ids|content|user|comment"), response))

      val future = bot.pagesById(Set(569559L, 4571809L)).revisions(Set.empty[Int], Set("ids", "content", "user", "comment"))
      val result = future.await
      result must have size 2
      result(0) === Page(Some(569559), 1, "Talk:Welfare reform", Seq(Revision(1, 569559).withUser(1, "u1").withComment("c1").withText(pageText1)))
      result(1) === Page(Some(4571809), 2, "User:Formator", Seq(Revision(2, 4571809).withUser(2, "u2").withComment("c2").withText(pageText2)))
    }
  }

  "no page" in {

    val response = """{
                     |    "batchcomplete": "",
                     |    "query": {
                     |        "pages": {
                     |            "-1": {
                     |                "ns": 0,
                     |                "title": "Absent",
                     |                "missing": ""
                     |            }
                     |        }
                     |    }
                     |}""".stripMargin

    val bot = getBot(HttpStub(
      Map(
        "titles" -> "Absent",
        "action" -> "query",
        "prop" -> "info|revisions",
        "continue" -> "", "rvlimit" -> "max",
        "rvprop" -> "ids|content|user|comment"), response))

    val future = bot.pagesByTitle(Set("Absent")).revisions(Set.empty[Int], Set("ids", "content", "user", "comment"))
    val result = future.await
    result must have size 1
    result.head === Page(None, 0, "Absent", missing = true)
  }

  def getBot(commands: HttpStub*) = {
    val http = new TestHttpClient(host, commands)

    new MwBotImpl(host, http)
  }

}
