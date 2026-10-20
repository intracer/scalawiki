package org.scalawiki.dto

import org.scalawiki.dto.ImageSpec._
import org.scalawiki.dto.markup.Template
import org.specs2.mutable.Specification

class ImageSpec extends Specification {

  "get author" should {
    "get author from wiki link" in {
      val wiki = makeTemplate("[[User:Qammer Wazir|Qammer Wazir]]")
      Image.getAuthorFromPage(wiki) === "Qammer Wazir"
    }

    "get author from plain text" in {
      val wiki = makeTemplate("PhotoAmateur")
      Image.getAuthorFromPage(wiki) === "PhotoAmateur"
    }

    "get author from link" in {
      val wiki = makeTemplate(
        "[http://wikimapia.org/#show=/user/2246978/ Alexander Savitsky]"
      )
      Image.getAuthorFromPage(wiki) === "Alexander Savitsky"
    }

    "get author from uk.wikipedia's {{Зображення}}" in {
      Image.getAuthorFromPage(zobrazhennya("Автор", "[[Користувач:Kamelot|Kamelot]]")) === "Kamelot"
    }

    "get author from {{Зображення}} with English parameters, copied from Commons" in {
      Image.getAuthorFromPage(zobrazhennya("Author", " [[User:Roman Zacharij|Roman Zacharij]]")) === "Roman Zacharij"
    }

    "prefer {{Information}}, but skip its empty author" in {
      Image.getAuthorFromPage(makeTemplate("[[User:A|A]]") + zobrazhennya("Автор", "[[Користувач:B|B]]")) === "A"
      Image.getAuthorFromPage(makeTemplate("") + zobrazhennya("Автор", "[[Користувач:B|B]]")) === "B"
    }

    "get no author from a page without a description template" in {
      Image.getAuthorFromPage("{{Art Photo|artist=|institution=Museum}}") === ""
    }
  }

  "fromPage" should {
    def page(wiki: String, uploader: String) =
      Page("File:Image.jpg").copy(
        revisions = Seq(Revision.one(wiki)),
        images = Seq(Image.basic("File:Image.jpg", None, Some(uploader), None, None, None, None, None, None))
      )

    "take the author from the page over the uploader" in {
      Image.fromPage(None)(page(zobrazhennya("Автор", "[[Користувач:Kiyanka|Kiyanka]]"), "AMY 81-412")).get.author ===
        Some("Kiyanka")
    }

    "leave the author to the caller when the page names none, or names them as Я" in {
      // the latest uploader may be a retouch or a bot's resize: ImageQuery looks up the original one
      Image.fromPage(None)(page("{{User:Butko/Info|location=Kyiv}}", "Butko")).get.author === None
      Image.fromPage(None)(page(zobrazhennya("Автор", "Я"), "Andrew J.Kurbiko")).get.author === None
    }
  }

  "authorFromUploader" should {
    "credit the original uploader" in {
      Image.authorFromUploader("Бахтін") === Some("Бахтін")
    }

    "not credit a bot" in {
      Image.authorFromUploader("ImageResizerBot") === None
      Image.authorFromUploader("BArchBot") === None
      Image.authorFromUploader("AHbot") === None
    }

    "apply the author renames" in {
      Image.authorFromUploader("ЯдвигаВереск") === Some("Wereskowa")
    }
  }

  "isVideo" should {
    "go by the media type" in {
      Image("File:Church.ogv", mediaType = Some("VIDEO")).isVideo === true
      Image("File:Bells.ogg", mediaType = Some("AUDIO")).isVideo === false
      Image("File:Church.jpg", mediaType = Some("BITMAP")).isVideo === false
    }

    "fall back to the extension" in {
      Image("File:Church.WEBM").isVideo === true
      Image("File:Church.ogv").isVideo === true
      Image("File:Bells.ogg").isVideo === false
      Image("File:Church.jpg").isVideo === false
    }
  }

  "fromPageRevision" should {
    "parse author and id" in {
      val wiki = makeTemplate(
        "[[User:PhotoMaster|PhotoMaster]]",
        "{{Monument|nature-park-id}}"
      )

      val page =
        Page("File:Image.jpg").copy(revisions = Seq(Revision.one(wiki)))
      val image = Image.fromPageRevision(page, Some("Monument")).get

      image.author === Some("PhotoMaster")
      image.monumentId === Some("nature-park-id")
    }

    "parse two monuments" in {
      val wiki = makeTemplate(
        "[[User:PhotoMaster|PhotoMaster]]",
        "{{Monument|id1}}{{Monument|id2}}"
      )

      val page = Page("File:Image.jpg").copy(revisions = Seq(Revision.one(wiki)))
      val image = Image.fromPageRevision(page, Some("Monument")).get

      image.author === Some("PhotoMaster")
      image.monumentId === Some("id1")
      image.monumentIds === Seq("id1", "id2")
    }

    "parse categories" in {

      val page = Page("File:Image.jpg").copy(revisions = Seq(Revision.one(withCategories)))

      val image = Image.fromPageRevision(page, Some("Monument")).get

      image.categories === Set(
        "Wiki loves monuments in Ukraine 2018 - Quality",
        "Uploaded via Campaign:wlm-ua",
        "Obviously ineligible submissions for WLM 2018 in Ukraine",
        "Saint Nicholas Church on Water"
      )
    }
  }

  "parse special nomination template" in {

    val page1 = Page("File:Image.jpg").copy(revisions = Seq(Revision.one(withSpecialNominations)))
    val image1 = Image
      .fromPageRevision(page1, Some("Monument"), Set("WLM2021-UA-Aero"))
      .get

    image1.specialNominations === Set("WLM2021-UA-Aero")

    val page2 =
      Page("File:Image.jpg").copy(revisions = Seq(Revision.one(withCategories)))
    val image2 = Image
      .fromPageRevision(page2, Some("Monument"), Set("WLM2021-UA-Aero"))
      .get

    image2.specialNominations === Set.empty

    val page3 = Page("File:Image.jpg").copy(revisions = Seq(Revision.one(withSpecialNomination2)))
    val image3 = Image
      .fromPageRevision(page3, Some("Monument"), Set("WLM2023-UA-interior"))
      .get

    image3.specialNominations === Set("WLM2023-UA-interior")
  }

  "resize" should {
    "be same" in {
      val (imageX, imageY) = (320, 200)
      val (boxX, boxY) = (320, 200)

      val px = Image.resizedWidth(imageX, imageY, boxX, boxY)
      px === 320
    }

    "divideBy2" in {
      val (imageX, imageY) = (640, 400)
      val (boxX, boxY) = (320, 200)

      val px = Image.resizedWidth(imageX, imageY, boxX, boxY)
      px === 320
    }

    "vertical divide by 2" in {
      val (imageX, imageY) = (400, 200)
      val (boxX, boxY) = (320, 200)

      val px = Image.resizedWidth(imageX, imageY, boxX, boxY)
      px === 320
    }
  }
}

object ImageSpec {

  def makeTemplate(author: String, description: String = ""): String =
    Template(
      "Information",
      Map(
        "description" -> description,
        "date" -> "",
        "source" -> "{{own}}",
        "author" -> author,
        "permission" -> "",
        "other versions" -> ""
      )
    ).text

  // uk.wikipedia's file description template, as its upload form writes it
  def zobrazhennya(authorParam: String, author: String): String =
    s"""{{Зображення
       ||Опис=Пам'ятник{{Monument Ukraine|44-141-0001}}
       ||Джерело=власне фото
       ||Час створення=2009
       ||$authorParam=$author
       ||Ліцензія={{PD-self}}
       |}}
       |""".stripMargin

  private val withCategories =
    """=={{int:filedesc}}==
      |{{Information
      ||description={{uk|1=Храм святителя-чудотворцая Миколи на водах в Києві на Подолі}}
      ||date=2015-01-11 14:19:34
      ||source={{own}}
      ||author=[[User:Yuri369|Yuri369]]
      ||permission=
      ||other versions=
      |}}
      |
      |=={{int:license-header}}==
      |{{self|cc-by-sa-4.0}}
      |
      |{{Wiki Loves Monuments 2018|ua}}
      |<!--[[Category:Wiki loves monuments in Ukraine 2018 - Quality]]-->
      |
      |[[Category:Uploaded via Campaign:wlm-ua]]
      |[[Category:Obviously ineligible submissions for WLM 2018 in Ukraine]]
      |[[Category:Saint Nicholas Church on Water]]
      |""".stripMargin

  private val withSpecialNominations =
    """=={{int:filedesc}}==
      |{{Information
      ||description={{uk|1=[[:uk:Замок Любарта|Єпископський будинок]], [[:uk:Луцьк|Луцьк]], [[:uk:Вулиця Кафедральна (Луцьк)|вул. Кафедральна]], 1а}}{{Monument Ukraine|07-101-0006}}[[Category:Wiki Loves Monuments in Ukraine 2021 - Quantity]]{{WLE2017-UA-SN|{{WLM2021-UA-Aero}}}}
      ||date=2021-08-04 12:42:19
      ||source={{own}}
      ||author=[[User:Mark Volkoff|Mark Volkoff]]
      ||permission=
      ||other versions=
      |}}
      |{{Location|50.739102|25.323562}}
      |
      |=={{int:license-header}}==
      |{{self|cc-by-sa-4.0}}
      |
      |{{Wiki Loves Monuments 2021|ua}}
      |
      |[[Category:Bishop's house in Lutsk castle]]
      |[[Category:Uploaded via Campaign:wlm-ua]]
      |""".stripMargin

  private val withSpecialNomination2 = """=={{int:filedesc}}==
                                          |{{Information
                                          ||description={{uk|1=Прибутковий будинок, [[:uk:Дніпро (місто)|Дніпро]], вул. Троїцька (Червона), 8}}{{Monument Ukraine|12-101-0443}}[[Category:Wiki Loves Monuments in Ukraine 2023 - Quantity]]{{WLM2023-UA-interior}}
                                          ||date=2023-07-18 08:59:48
                                          ||source={{own}}
                                          ||author=[[User:Olebesedin|Olebesedin]]
                                          ||permission=
                                          ||other versions=
                                          |}}
                                          |{{Location|48.462144|35.042819}}
                                          |
                                          |=={{int:license-header}}==
                                          |{{self|cc-by-sa-4.0}}
                                          |
                                          |{{Wiki Loves Monuments 2023|ua}}
                                          |
                                          |[[Category:8 Troitska Street, Dnipro]]
                                          |[[Category:Uploaded via Campaign:wlm-ua]]
                                          |""".stripMargin
}
