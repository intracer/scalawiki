package org.scalawiki.dto.markup

import org.scalawiki.dto.Image
import org.specs2.mutable.Specification

class GalleryHtmlSpec extends Specification {

  "gallery as html" should {

    "be without descriptions" in {
      val title = "1.jpg"
      val images = Seq(Image(s"File:$title",
        url = Some(s"http://domain/path/$title")
      ))

      Gallery.asHtml(images) ===
        s"""|<ul class="gallery mw-gallery-traditional">
            |<li class="gallerybox">
            |  <div class="thumb">
            |    <a href="http://domain/path/1.jpg">
            |       <img class="cropped" alt="File:1.jpg" src="http://domain/path/1.jpg">
            |    </a>
            |  </div>
            |</li>
            |</ul>""".stripMargin
    }

    "be with descriptions" in {
      val title = "1.jpg"
      val description = title + " description"
      val images = Seq(Image(s"File:$title",
        url = Some(s"http://domain/path/$title")
      ))

      Gallery.asHtml(images, Seq(description)) ===
        s"""|<ul class="gallery mw-gallery-traditional">
            |<li class="gallerybox">
            |  <div class="thumb">
            |    <a href="http://domain/path/1.jpg">
            |       <img class="cropped" alt="File:1.jpg" src="http://domain/path/1.jpg">
            |    </a>
            |  </div>
            |  <div class="gallerytext">
            |    <p>$description</p>
            |  </div>
            |</li>
            |</ul>""".stripMargin
    }
  }
}
