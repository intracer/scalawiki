package org.scalawiki.dto.markup

import org.scalawiki.dto.Image
import org.specs2.mutable.Specification

class GalleryThumbSpec extends Specification {

  "thumbUrl" should {
    "preserve url" in {
      val url = "https://host/image.jpg"
      val image = new Image("image.jpg", url = Some(url), width = Some(1024), height = Some(768))

      Gallery.thumbUrl(image, 800, 600, false) === url
    }

    "preserve large image url" in {
      val url = "https://host/image.jpg"
      val image = new Image("image.jpg", url = Some(url), width = Some(1024), height = Some(768))

      Gallery.thumbUrl(image, 1024, 768, true) === url
    }


    "resize url" in {
      val url = "https://host/image.jpg"
      val image = new Image("image.jpg", url = Some(url), width = Some(1024), height = Some(768))

      Gallery.thumbUrl(image, 800, 600, true) === "https://host/image.jpg/800px-image.jpg"
    }

    "resize long url" in {
      val title = Seq.fill(162)("a").mkString + ".jpg"
      val url = s"https://host/$title"
      val image = new Image(title, url = Some(url), width = Some(1024), height = Some(768))

      Gallery.thumbUrl(image, 800, 600, true) === s"https://host/$title/800px-thumbnail.jpg"
    }

    "resize sublong url" in {
      val title = Seq.fill(161)("a").mkString + ".jpg"
      val url = s"https://host/$title"
      val image = new Image(title, url = Some(url), width = Some(1024), height = Some(768))

      Gallery.thumbUrl(image, 800, 600, true) === s"https://host/$title/800px-$title"
    }

    "resize utf-8 url" in {
      val title = Seq.fill(161)("я").mkString + ".jpg"
      val url = s"https://host/$title"
      val image = new Image(title, url = Some(url), width = Some(1024), height = Some(768))

      Gallery.thumbUrl(image, 800, 600, true) === s"https://host/$title/800px-thumbnail.jpg"
    }

    "resize pdf url" in {
      val title = "document.pdf"
      val url = s"https://host/$title"
      val image = new Image(title, url = Some(url), width = Some(1024), height = Some(768))

      Gallery.thumbUrl(image, 800, 600, true) === s"https://host/$title/page1-800px-$title.jpg"
    }

    "resize tiff url" in {
      val title = "image.tif"
      val url = s"https://host/$title"
      val image = new Image(title, url = Some(url), width = Some(1024), height = Some(768))

      Gallery.thumbUrl(image, 800, 600, true) === s"https://host/$title/lossy-page1-800px-$title.jpg"
    }

  }
}

