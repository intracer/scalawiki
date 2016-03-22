package org.scalawiki.dto.markup

import org.scalawiki.dto.Image
import org.specs2.mutable.Specification

class GalleryWikiSpec extends Specification {

  "gallery as wiki" should {

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
