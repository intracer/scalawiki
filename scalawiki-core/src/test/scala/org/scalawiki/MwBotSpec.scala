package org.scalawiki

import org.scalawiki.util.{Command, MockBotSpec}
import org.specs2.mutable.Specification
import spray.util.pimpFuture

class MwBotSpec extends Specification with MockBotSpec {

  "get page text" should {
    "return a page text" in {
      val pageText = "some vandalism"

      val bot = getBot(new Command(Map("title" -> "PageTitle", "action" -> "raw"), pageText, "/w/index.php"))

      bot.pageText("pageTitle").await === pageText
    }
  }

  "get missing page text" should {
    "return error" in {

      val bot = getBot(new Command(Map("title" -> "PageTitle", "action" -> "raw"), null, "/w/index.php"))

      bot.pageText("pageTitle").await === "" // TODO error
    }
  }
}



