package org.scalawiki.dto.markup

import org.scalawiki.dto.Image

object Gallery {

  def asWiki(images: Seq[String], descriptions: Seq[String] = Seq.empty): String = {
    val fill = if (descriptions.size < images.size) {
      Seq.fill(images.size - descriptions.size)("")
    } else
      Seq.empty

    images.zip(descriptions ++ fill)
      .map {
        case (image, description) =>
          val filed = (if (!image.startsWith("File:")) "File:" else "") + image

          val piped = if (description.nonEmpty) " | " + description else ""

          filed + piped
      }
      .mkString("<gallery>\n", "\n", "\n</gallery>")
  }

  def asHtml(images: Seq[Image], descriptions: Seq[String] = Seq.empty): String = {
    val width = 300
    val height = 200
    val fill = if (descriptions.size < images.size) {
      Seq.fill(images.size - descriptions.size)("")
    } else
      Seq.empty

    images.zip(descriptions ++ fill)
      .map {
        case (image, description) =>

          s"""|<li class="gallerybox">
              |<div class="thumb">
              |<a href="${image.pageUrl.getOrElse("")}" >
              | <img class="cropped" alt="${image.title}" src="${thumbUrl(image, width, height, thumbUrl = false)}">
              |</a>
              |</div>
              |</li>""".stripMargin

      }.mkString("<ul class=\"gallery mw-gallery-traditional\">\n", "\n", "\n</ul>")
  }

  def thumbUrl(image: Image, resizeToWidth: Int, resizeToHeight: Int, thumbUrl: Boolean = true): String = {
    val url = image.url.get
    if (!thumbUrl) {
      url
    } else {
      val h = image.height.get
      val w = image.width.get

      val px = image.resizeTo(resizeToWidth, resizeToHeight)

      val isPdf = image.title.toLowerCase.endsWith(".pdf")
      val isTif = image.title.toLowerCase.endsWith(".tif")

      if (px < w || isPdf || isTif) {
        val lastSlash = url.lastIndexOf("/")
        val utf8Size = image.title.getBytes("UTF8").length
        val thumbStr = if (utf8Size > 165) {
          "thumbnail.jpg"
        } else {
          url.substring(lastSlash + 1)
        }
        url.replace("//upload.wikimedia.org/wikipedia/commons/", "//upload.wikimedia.org/wikipedia/commons/thumb/") + "/" +
          (if (isPdf) "page1-"
          else if (isTif) "lossy-page1-"
          else
            "") +
          px + "px-" + thumbStr + (if (isPdf || isTif) ".jpg" else "")
      } else {
        url
      }
    }
  }
}
