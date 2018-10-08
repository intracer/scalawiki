package org.scalawiki.dto

import java.nio.file.{Files, Paths}
import java.time.{LocalDateTime, ZoneOffset, ZonedDateTime}
import java.time.format.DateTimeFormatter

import org.scalawiki.MwBot
import org.scalawiki.dto.markup.Gallery
import org.scalawiki.wikitext.TemplateParser

case class ImageMetadata(data: Map[String, String]) {

  val pattern = "yyyy:MM:dd HH:mm:ss"
  val df = DateTimeFormatter.ofPattern(pattern)

  def camera: Option[String] = data.get("Model")

  def date: Option[ZonedDateTime] = data.get("DateTime")
    .map(s => LocalDateTime.parse(s, df).atZone(ZoneOffset.UTC))
}

case class Image(title: String,
                 url: Option[String] = None,
                 pageUrl: Option[String] = None,
                 size: Option[Long] = None,
                 width: Option[Int] = None,
                 height: Option[Int] = None,
                 author: Option[String] = None,
                 uploader: Option[User] = None,
                 year: Option[String] = None,
                 date: Option[ZonedDateTime] = None,
                 monumentId: Option[String] = None,
                 pageId: Option[Long] = None,
                 metadata: Option[ImageMetadata] = None,
                 categories: Set[String] = Set.empty
                ) extends Ordered[Image] {

  def compare(that: Image) = title.compareTo(that.title)

  //  def region: Option[String] = monumentId.map(_.split("-")(0))

  def download(filename: String) {
    import scala.concurrent.ExecutionContext.Implicits.global
    for (bytes <- MwBot.fromSite(Site.commons).getByteArray(url.get))
      Files.write(Paths.get(filename), bytes)
  }

  def pixels: Option[Long] = for (w <- width; h <- height) yield  w * h

  def mpx: Option[Double] = pixels.map(_ / Math.pow(10, 6))

  def mpxStr: String = mpx.fold("")(v => f"$v%1.2f Mpx ")

  def resolution: Option[String] = for (w <- width; h <- height) yield  w + " x " + h

  def resizeTo(resizeToX: Int, resizeToY: Int): Int =
    Image.resizedWidth(width.get, height.get, resizeToX, resizeToY)

}

object Image {

  val categoryRegex = "\\[\\[Category:([^]]+)\\]\\]".r

  def fromPageImages(page: Page): Option[Image] =
    page.images.headOption

  def fromPageRevision(page: Page, monumentIdTemplate: Option[String]): Option[Image] = {
    page.revisions.headOption.map { revision =>

      val content = revision.content.getOrElse("")
      val idOpt = TemplateParser.parseOne(content, monumentIdTemplate).flatMap(_.getParamOpt("1"))

      val author = getAuthorFromPage(content)

      val categories = categoryRegex.findAllIn(content).matchData.map(_.group(1)).toSet

      new Image(page.title,
        author = Some(author),
        date = revision.timestamp,
        monumentId = idOpt,
        pageId = page.id,
        categories = categories)
    }
  }

  def fromPage(page: Page, monumentIdTemplate: Option[String]): Option[Image] = {
    for (fromImage <- Image.fromPageImages(page);
         fromRev <- Image.fromPageRevision(page, monumentIdTemplate))
      yield fromImage.copy(monumentId = fromRev.monumentId, author = fromRev.author, categories = fromRev.categories)
  }

  def getAuthorFromPage(content: String): String = {
    val template = TemplateParser.parseOne(content, Some("Information"))
    val authorValue = template.flatMap { t =>
      t.getParamOpt("author").orElse(t.getParamOpt("Author"))
    }.getOrElse("")

    parseUser(authorValue)
  }

  def parseUser(authorValue: String): String = {
    val i1: Int = authorValue.indexOf("User:")
    val i2: Int = authorValue.indexOf("user:")
    val start = Seq(i1, i2, Int.MaxValue).filter(_ >= 0).min

    if (start < Int.MaxValue) {
      val pipe = authorValue.indexOf("|", start)
      val end = if (pipe >= 0)
        pipe
      else authorValue.length
      authorValue.substring(start + "user:".length, end)
    }
    else
      authorValue
  }

  def basic(title: String,
            timestamp: Option[ZonedDateTime],
            uploader: Option[String],
            size: Option[Long],
            width: Option[Int],
            height: Option[Int],
            url: Option[String],
            pageUrl: Option[String],
            pageId: Option[Long],
            metadata: Option[Map[String, String]] = None)
  = new Image(
    title = title,
    date = timestamp,
    uploader = uploader.map(name => User(None, Some(name))),
    size = size,
    width = width,
    height = height,
    url = url,
    pageUrl = pageUrl,
    pageId = pageId,
    metadata = metadata.map(ImageMetadata.apply))

  def gallery(images: Seq[String], descriptions: Seq[String] = Seq.empty): String =
    Gallery.asWiki(images, descriptions)

  def resizedWidth(w: Int, h: Int, resizeToX: Int, resizeToY: Int): Int = {
    val xRatio = w.toDouble / resizeToX
    val yRatio = h.toDouble / resizeToY

    val width = Math.min(resizeToX, w / yRatio)
    width.toInt
  }
}
