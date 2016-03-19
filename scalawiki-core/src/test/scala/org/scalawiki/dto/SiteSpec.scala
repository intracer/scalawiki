package org.scalawiki.dto

import org.specs2.mutable.Specification

class SiteSpec extends Specification {

  "host" should {
    "wikipedia" in {
      Site.host("en.wikipedia.org") === Site(Some("en"), "wikipedia", "en.wikipedia.org", "https", "/w")
    }

    "wikimedia" in {
      Site.host("commons.wikimedia.org") === Site(None, "commons", "commons.wikimedia.org", "https", "/w")
    }
  }

}
