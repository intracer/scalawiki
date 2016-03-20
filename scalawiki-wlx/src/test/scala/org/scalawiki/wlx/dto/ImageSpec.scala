package org.scalawiki.wlx.dto

import org.scalawiki.dto.markup.Template
import org.scalawiki.dto.{Image, Page, Revision}
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

  }

  "fromPageRevision" should {
    "parse" in {
      val wiki = makeTemplate("[[User:PhotoMaster|PhotoMaster]]", "{{Monument|nature-park-id}}")

      val page = Page("File:Image.jpg").copy(revisions = Seq(Revision.one(wiki)))
      val image = Image.fromPageRevision(page, Some("Monument")).get

      image.author === Some("PhotoMaster")
      image.monumentId === Some("nature-park-id")
    }
  }

  "gallery" should {

    "be without descriptions" in {
      val images = (1 to 3).map(i => s"File:$i.jpg")

      Image.gallery(images) ===
        """<gallery>
          |File:1.jpg
          |File:2.jpg
          |File:3.jpg
          |</gallery>""".stripMargin
    }

    "be with descriptions" in {
      val images = (1 to 3).map(i => s"File:$i.jpg")
      val descriptions = (1 to 3).map("Description " + _)

      Image.gallery(images, descriptions) ===
        """<gallery>
          |File:1.jpg | Description 1
          |File:2.jpg | Description 2
          |File:3.jpg | Description 3
          |</gallery>""".stripMargin
    }

    "add File:" in {
      val images = (1 to 3).map(_ + ".jpg")
      val descriptions = (1 to 3).map("Description " + _)

      Image.gallery(images, descriptions) ===
        """<gallery>
          |File:1.jpg | Description 1
          |File:2.jpg | Description 2
          |File:3.jpg | Description 3
          |</gallery>""".stripMargin
    }
  }

}
