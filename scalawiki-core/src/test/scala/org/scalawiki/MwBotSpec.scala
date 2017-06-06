package org.scalawiki

import org.scalawiki.util.{HttpStub, MockBotSpec}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import spray.util.pimpFuture

class MwBotSpec extends Specification with MockBotSpec with Mockito {

  "get page text" should {
    "return a page text" in {
      val pageText = "some vandalism"

      val bot = getBot(new HttpStub(Map("title" -> "PageTitle", "action" -> "raw"), pageText, "/w/index.php"))

      bot.pageText("pageTitle").await === pageText
    }
  }

  "get missing page text" should {
    "return error" in {

      val bot = getBot(new HttpStub(Map("title" -> "PageTitle", "action" -> "raw"), null, "/w/index.php"))

      bot.pageText("pageTitle").await === "" // TODO error
    }
  }

  "generator to version" should {
    import MediaWikiVersion._
    "parse out prefix" in {
      fromGenerator("MediaWiki 1.26.2") === MediaWikiVersion("1.26")
    }

    "parse out suffix" in {
      fromGenerator("MediaWiki 1.27.0-wmf.19") === MediaWikiVersion("1.27")
    }

    "parse out Debian suffix" in {
      fromGenerator("MediaWiki 1.19.20+dfsg-0+deb7u3") === MediaWikiVersion("1.19")
    }

    "parse custom to unknow" in {
      fromGenerator("MediaWiki") === MediaWikiVersion.UNKNOWN
    }
  }

  "MediaWikiVersion" should {
    "be ordered" in {
      MediaWikiVersion("1.24") must be < MediaWikiVersion("1.25")
      MediaWikiVersion("1.19") must be < MediaWikiVersion("1.24")
    }
  }
}