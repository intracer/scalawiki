package org.scalawiki.bots.museum

import net.ceedubs.ficus.FicusConfig
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

    "get dir, article, wlmId and extra column" in {
      Entry.fromRow(Seq("dir", "article", "wlmId", "something else")) === Entry("dir", Some("article"), Some("wlmId"))
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
      Entry("dir",
        images = Seq(
          EntryImage("image", Some("description"))
        )
      ).imagesMaps === Seq(
        Map(
          "title" -> "dir 1",
          "file" -> "image",
          "description" -> "{{uk|description, [[:uk:dir|]]}}",
          "source-description" -> "description"
        )
      )
    }

    "use article" in {
      Entry("dir",
        Some("article"),
        images = Seq(
          EntryImage("image", Some("description"))
        )
      ).imagesMaps === Seq(
        Map(
          "title" -> "article 1",
          "file" -> "image",
          "description" -> "{{uk|description, [[:uk:article|]]}}",
          "source-description" -> "description"
        )
      )
    }
  }

  "to/fromConfig" should {

    def roundTrip(entry: Entry, dir: String): Entry =
      Entry.fromConfig(entry.toConfig, dir)

    "map dir to article" in {
      import net.ceedubs.ficus.Ficus._

      val cfg: FicusConfig = Entry("dir").toConfig
      cfg.as[String]("article") === "dir"
      cfg.as[Seq[String]]("images") === Seq.empty
    }

    "map dir" in {
      val entry = Entry("dir", Some("article"))
      roundTrip(entry, "dir") === entry
    }

    "read image" in {
      val entry = Entry("dir", article = Some("article"),
        images = Seq(
          EntryImage("image", Some("description"))
        ))
      roundTrip(entry, "dir") === entry
    }

  }
}
