package org.scalawiki

import org.scalawiki.util.{HttpStub, MockBotSpec, TestUtils}
import org.specs2.mutable.Specification
import spray.util.pimpFuture

class EditSpec extends Specification with MockBotSpec {

  val tokenResponse = """{"batchcomplete":"","query":{"tokens":{"csrftoken":"cafebabe+\\"}}}"""
  val successResponse = """{"edit":{"result":"Success","pageid":1776370,"title":"pageTitle","contentmodel":"wikitext"}}"""

  "bot" should {
    "edit successfully" in {
      val siteInfo = TestUtils.resourceAsString("/org/scalawiki/ukwiki_siteinfo.json")
      val bot: MwBot = getBot(
        HttpStub(Map("action" -> "query", "meta" -> "siteinfo", "format" -> "json"), siteInfo),
        HttpStub(Map("action" -> "query", "meta" -> "tokens", "format" -> "json"), tokenResponse),
          HttpStub(Map("assert" -> "bot", "format" -> "json", "text" -> "pageText", "token"-> "cafebabe+\\", "bot" -> "x",
            "title" -> "pageTitle", "action" -> "edit", "summary" -> "editsummary"), successResponse)
      )

      val result = bot.page("pageTitle").edit("pageText", Some("editsummary")).await
      result === "Success"
    }
  }

}
