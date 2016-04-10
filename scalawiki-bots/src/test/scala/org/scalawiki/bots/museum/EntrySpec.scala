package org.scalawiki.bots.museum

import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.FicusConfig
import org.specs2.mutable.Specification

class EntrySpec extends Specification {

  val entry0 = Entry("dir", article = Some("article"))

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
      ).genImageFields.imagesMaps === Seq(
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
      ).genImageFields.imagesMaps === Seq(
        Map(
          "title" -> "article 1",
          "file" -> "image",
          "description" -> "{{uk|description, [[:uk:article|]]}}",
          "source-description" -> "description"
        )
      )
    }

    "use parent wlm id" in {
      Entry("dir",
        Some("article"),
        images = Seq(
          EntryImage("image", Some("description"))
        ),
        wlmId = Some("parent-wlm-id")
      ).genImageFields.imagesMaps === Seq(
        Map(
          "title" -> "article 1",
          "file" -> "image",
          "description" -> "{{uk|description, [[:uk:article|]]}} {{Monument Ukraine|parent-wlm-id}}",
          "source-description" -> "description"
        )
      )
    }

    "override parent wlm id" in {
      val entry = Entry("dir",
        Some("article"),
        images = Seq(
          EntryImage("image1", Some("description1"), wlmId = None),
          EntryImage("image2", Some("description2"), wlmId = Some("specific-wlm-id"))
        ),
        wlmId = Some("parent-wlm-id")
      ).genImageFields

      val maps = entry.imagesMaps

      maps.size === 2

      maps.head === Map(
        "title" -> "article 1",
        "file" -> "image1",
        "description" -> "{{uk|description1, [[:uk:article|]]}} {{Monument Ukraine|parent-wlm-id}}",
        "source-description" -> "description1"
      )

      maps.last === Map(
        "title" -> "article 2",
        "file" -> "image2",
        "description" -> "{{uk|description2, [[:uk:article|]]}} {{Monument Ukraine|specific-wlm-id}}",
        "source-description" -> "description2",
        "wlm-id" -> "specific-wlm-id"
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
      roundTrip(entry0, "dir") === entry0
    }

    "read image" in {
      val entry = entry0.copy(
        images = Seq(
          EntryImage("image", Some("description"))
        )).genImageFields

      roundTrip(entry, "dir") === entry
    }

    "read parent wlmId" in {
      val entry = entry0.copy(wlmId = Some("wlm-id"),
        images = Seq(
          EntryImage("image", Some("description"))
        )).genImageFields

      roundTrip(entry, "dir") === entry
    }

    "read entry and image wlmId" in {
      val entry = entry0.copy(wlmId = Some("parent-wlm-id"),
        images = Seq(
          EntryImage("image", Some("description"), wlmId = Some("image-wlm"))
        )).genImageFields

      roundTrip(entry, "dir") === entry
    }

    "read uploadTitle" in {
      val origEntry = Entry("dir", article = Some("article"),
        images = Seq(
          EntryImage("image1", Some("description1"))
        )).genImageFields

      val str = origEntry.toConfig.root().render().replace("article 1", "article 2")
      val cfg = ConfigFactory.parseString(str)
      val entry = Entry.fromConfig(cfg, "dir")
      val image = entry.images.head
      image.uploadTitle === Some("article 2")
    }

    "read wiki-description" in {
      val origEntry = Entry("dir", article = Some("article"),
        images = Seq(
          EntryImage("image1", Some("description1"))
        )).genImageFields

      val str = origEntry.toConfig.root().render().replace("{{uk|description1", "{{uk|description2")
      val cfg = ConfigFactory.parseString(str)
      val entry = Entry.fromConfig(cfg, "dir")
      val image = entry.images.head
      image.wikiDescription === Some("{{uk|description2, [[:uk:article|]]}}")
    }
  }

  "diff reporter" should {

    "be quite" in {
      val image = EntryImage("image", Some("description1"), Some("wiki-description1"))
      val entry = entry0.copy(wlmId = Some("wlm-id"), images = Seq(image))

      entry.diff(entry) === Seq.empty
    }

    "tell image entry field change" in {

      val image = EntryImage("image", Some("description1"), wikiDescription = Some("wiki-description1"))
      val entry = entry0.copy(wlmId = Some("wlm-id"), images = Seq(image))

      val changed = entry.copy(images = Seq(image.copy(wikiDescription = Some("wiki-description2"))))

      entry.diff(changed) === Seq(Diff("wikiDescription", Some("wiki-description1"), Some("wiki-description2")))
    }

    "tell wlm id change" in {
      val entry = entry0.copy(wlmId = Some("wlm-id1"))
      val changed = entry.copy(wlmId = Some("wlm-id2"))
      entry.diff(changed) === Seq(Diff("wlmId", Some("wlm-id1"), Some("wlm-id2")))
    }
  }
}
