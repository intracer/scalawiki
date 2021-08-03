package org.scalawiki.dto

import org.scalawiki.dto.markup.Template
import org.specs2.mutable.Specification

class ImageSpec extends Specification {

  def makeTemplate(author: String, description: String = "") =
    new Template("Information",
      Map(
        "description" -> description,
        "date" -> "",
        "source" -> "{{own}}",
        "author" -> author,
        "permission" -> "",
        "other versions" -> ""
      )
    ).text

  "get Author" should {
    "get author from wiki link" in {
      val wiki = makeTemplate("[[User:Qammer Wazir|Qammer Wazir]]")
      Image.getAuthorFromPage(wiki) === "Qammer Wazir"
    }

    "get author from plain text" in {
      val wiki = makeTemplate("PhotoAmateur")
      Image.getAuthorFromPage(wiki) === "PhotoAmateur"
    }

    "get author from link" in {
      val wiki = makeTemplate("[http://wikimapia.org/#show=/user/2246978/ Alexander Savitsky]")
      Image.getAuthorFromPage(wiki) === "Alexander Savitsky"
    }

  }

  "fromPageRevision" should {
    "parse author and id" in {
      val wiki = makeTemplate("[[User:PhotoMaster|PhotoMaster]]", "{{Monument|nature-park-id}}")

      val page = Page("File:Image.jpg").copy(revisions = Seq(Revision.one(wiki)))
      val image = Image.fromPageRevision(page, Some("Monument")).get

      image.author === Some("PhotoMaster")
      image.monumentId === Some("nature-park-id")
    }

    "parse two monuments" in {
      val wiki = makeTemplate("[[User:PhotoMaster|PhotoMaster]]", "{{Monument|id1}}{{Monument|id2}}")

      val page = Page("File:Image.jpg").copy(revisions = Seq(Revision.one(wiki)))
      val image = Image.fromPageRevision(page, Some("Monument")).get

      image.author === Some("PhotoMaster")
      image.monumentId === Some("id1")
      image.monumentIds === Seq("id1", "id2")
    }

    "parse categories" in {
      val text = """=={{int:filedesc}}==
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
      val page = Page("File:Image.jpg").copy(revisions = Seq(Revision.one(text)))

      val image = Image.fromPageRevision(page, Some("Monument")).get

      image.categories === Set("Wiki loves monuments in Ukraine 2018 - Quality",
        "Uploaded via Campaign:wlm-ua",
        "Obviously ineligible submissions for WLM 2018 in Ukraine",
        "Saint Nicholas Church on Water")
    }
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
