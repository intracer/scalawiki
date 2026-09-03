package org.scalawiki

import org.scalawiki.util.{HttpStub, MockBotSpec, TestUtils}
import org.specs2.mutable.Specification
import spray.util.pimpFuture

class EditSpec extends Specification with MockBotSpec {

  val tokenResponse =
    """{"batchcomplete":"","query":{"tokens":{"csrftoken":"cafebabe+\\"}}}"""
  val successResponse =
    """{"edit":{"result":"Success","pageid":1776370,"title":"pageTitle","contentmodel":"wikitext"}}"""
  val errorResponse =
    """{"edit":{"result":"Error","pageid":1776370,"title":"pageTitle","contentmodel":"wikitext"}}"""

  "bot" should {
    "edit successfully" in {
      val siteInfo =
        TestUtils.resourceAsString("/org/scalawiki/ukwiki_siteinfo.json")
      val bot: MwBot = getBot(
        HttpStub(
          Map("action" -> "query", "meta" -> "siteinfo", "format" -> "json"),
          siteInfo
        ),
        HttpStub(
          Map("action" -> "query", "meta" -> "tokens", "format" -> "json"),
          tokenResponse
        ),
        HttpStub(
          Map(
            "assert" -> "bot",
            "format" -> "json",
            "text" -> "pageText",
            "token" -> "cafebabe+\\",
            "bot" -> "x",
            "title" -> "pageTitle",
            "action" -> "edit",
            "summary" -> "editsummary"
          ),
          successResponse
        )
      )

      val result =
        bot.page("pageTitle").edit("pageText", Some("editsummary")).await
      result === "Success"
    }

    "edit retry error once" in {
      val siteInfo =
        TestUtils.resourceAsString("/org/scalawiki/ukwiki_siteinfo.json")
      val bot: MwBot = getBot(
        HttpStub(
          Map("action" -> "query", "meta" -> "siteinfo", "format" -> "json"),
          siteInfo
        ),
        HttpStub(
          Map("action" -> "query", "meta" -> "tokens", "format" -> "json"),
          tokenResponse
        ),
        HttpStub(
          Map(
            "assert" -> "bot",
            "format" -> "json",
            "text" -> "pageText",
            "token" -> "cafebabe+\\",
            "bot" -> "x",
            "title" -> "pageTitle",
            "action" -> "edit",
            "summary" -> "editsummary"
          ),
          errorResponse
        ),
        HttpStub(
          Map(
            "assert" -> "bot",
            "format" -> "json",
            "text" -> "pageText",
            "token" -> "cafebabe+\\",
            "bot" -> "x",
            "title" -> "pageTitle",
            "action" -> "edit",
            "summary" -> "editsummary"
          ),
          successResponse
        )
      )

      val result =
        bot.page("pageTitle").edit("pageText", Some("editsummary")).await
      result === "Success"
    }

    "refresh an expired CSRF token and retry on badtoken" in {
      val siteInfo =
        TestUtils.resourceAsString("/org/scalawiki/ukwiki_siteinfo.json")
      def tokenResponseOf(t: String) =
        s"""{"batchcomplete":"","query":{"tokens":{"csrftoken":"$t"}}}"""
      val badTokenResponse =
        """{"error":{"code":"badtoken","info":"Invalid CSRF token.","*":""}}"""
      def editStub(token: String, response: String) =
        HttpStub(
          Map(
            "assert" -> "bot",
            "format" -> "json",
            "text" -> "pageText",
            "token" -> token,
            "bot" -> "x",
            "title" -> "pageTitle",
            "action" -> "edit",
            "summary" -> "editsummary"
          ),
          response
        )

      val bot: MwBot = getBot(
        HttpStub(
          Map("action" -> "query", "meta" -> "siteinfo", "format" -> "json"),
          siteInfo
        ),
        HttpStub(
          Map("action" -> "query", "meta" -> "tokens", "format" -> "json"),
          tokenResponseOf("stale-token")
        ),
        editStub("stale-token", badTokenResponse),
        // token was invalidated -> a fresh one is fetched for the retry
        HttpStub(
          Map("action" -> "query", "meta" -> "tokens", "format" -> "json"),
          tokenResponseOf("fresh-token")
        ),
        editStub("fresh-token", successResponse)
      )

      val result =
        bot.page("pageTitle").edit("pageText", Some("editsummary")).await
      result === "Success"
    }
  }

}
