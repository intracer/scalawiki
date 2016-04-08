package org.scalawiki.bots.museum

import org.specs2.mutable.Specification

class EntrySpec extends Specification {

  "fromRow" should {
    "get dir" in {
      Entry.fromRow(Seq("dir")) === Entry("dir")
    }

    "get dir and article" in {
      Entry.fromRow(Seq("dir", "article")) === Entry("dir", Some("article"))
    }

    "get dir, article and wlmId" in {
      Entry.fromRow(Seq("dir", "article", "wlmId")) === Entry("dir", Some("article"), Some("wlmId"))
    }

    "get dir, empty article and empty wlmId" in {
      Entry.fromRow(Seq("dir", " ", " ")) === Entry("dir")
    }

    "get dir, article and empty wlmId" in {
      Entry.fromRow(Seq("dir", "article", " ")) === Entry("dir", Some("article"))
    }
  }

  "imagesMaps" should {
    "be empty when no images" in {
      Entry("dir").imagesMaps === Seq.empty
    }

    "default article to dir" in {
      Entry("dir", images = Seq("image"), descriptions = Seq("description")).imagesMaps === Seq(
        Map(
          "title" -> "dir 1",
          "file" -> "image",
          "description" -> "{{uk|description, [[:uk:dir|]]}}"
        )
      )
    }

    "use article" in {
      Entry("dir", Some("article"), images = Seq("image"), descriptions = Seq("description")).imagesMaps === Seq(
        Map(
          "title" -> "article 1",
          "file" -> "image",
          "description" -> "{{uk|description, [[:uk:article|]]}}"
        )
      )
    }
  }
}
