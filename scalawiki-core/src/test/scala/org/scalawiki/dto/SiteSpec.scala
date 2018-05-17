package org.scalawiki.dto

import org.specs2.mutable.Specification

class SiteSpec extends Specification {

  "host" should {
    "wikipedia" in {
      Site.host("en.wikipedia.org") === Site(Some("en"), "wikipedia", "en.wikipedia.org", "https", None, "/w")
    }

    "wikimedia" in {
      Site.host("commons.wikimedia.org") === Site(None, "commons", "commons.wikimedia.org", "https", None, "/w")
    }
  }

  "pageUrl" should {
    "commons page url" in {
      Site.commons.pageUrl("File:Image.jpg") === "https://commons.wikimedia.org/wiki/File:Image.jpg"
    }

    "localhost page url" in {
      Site.localhost.pageUrl("File:Image.jpg") === "http://localhost/mediawiki/index.php/File:Image.jpg"
    }
  }
}
